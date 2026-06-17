# 03 — Luồng Saga đặt hàng (phần quan trọng nhất)

Đây là phần tinh tuý nhất để hiểu microservices. Hãy đọc kỹ.

## 1. Vấn đề: giao dịch phân tán

Đặt một đơn hàng cần 3 việc, ở 3 service + 3 database khác nhau:
1. Tạo đơn (order-service / orderdb)
2. Trừ tiền (payment-service / paymentdb)
3. Trừ kho (product-service / productdb)

Trong monolith, ta bọc cả 3 trong **một transaction ACID** — lỗi thì rollback hết. Nhưng ở microservices, **không có transaction xuyên service/database**. Nếu trừ tiền xong mà trừ kho thất bại (hết hàng) thì sao? → Phải **hoàn tiền**.

Giải pháp: **Saga Pattern** — chuỗi các giao dịch cục bộ, mỗi bước có một **hành động bù trừ (compensation)** để hoàn tác khi bước sau thất bại. ShopMicro dùng **Saga kiểu choreography** (các service tự phối hợp qua sự kiện Kafka, không có "nhạc trưởng" trung tâm).

## 2. Sơ đồ luồng đầy đủ

```
        ┌──────────────┐
Client ►│ order-service│ tạo đơn PENDING, lưu orderdb
        └──────┬───────┘
               │ ① phát "order.created"
               ▼
            [ Kafka ]
               │
               ▼
        ┌──────────────┐
        │payment-service│ ② nghe "order.created" -> trừ tiền
        └──────┬───────┘
               │
        ┌──────┴───────────────────────┐
        ▼ (đủ tiền)                     ▼ (lỗi)
   phát "payment.completed"        phát "payment.failed"
        │                               │
        ▼                               ▼
 ┌──────────────┐                ┌──────────────┐
 │product-service│               │ order-service│ -> đơn CANCELLED
 │ ③ trừ kho     │               │ (không cần bù trừ vì chưa trừ kho)
 └──────┬───────┘                └──────────────┘
        │
 ┌──────┴──────────┐
 ▼ (còn hàng)       ▼ (hết hàng)
"inventory.reserved" "inventory.failed"
        │                  │
        ▼                  ▼
 ┌──────────────┐   ┌────────────────────────────────┐
 │ order-service│   │ order-service:                  │
 │ -> CONFIRMED │   │  - đơn -> CANCELLED             │
 │ phát         │   │  - phát "payment.refund" ◄─ BÙ TRỪ
 │ "order.      │   │    (payment-service hoàn tiền)  │
 │  confirmed"  │   │  - phát "order.cancelled"       │
 └──────┬───────┘   └──────────┬─────────────────────┘
        │                      │
        └───────► [ Kafka ] ◄──┘
                     │
                     ▼
            ┌──────────────────┐
            │notification-service│ gửi email xác nhận / huỷ
            └──────────────────┘
```

## 3. Đối chiếu sự kiện ↔ code

| Bước | Topic Kafka | Service phát | Service nghe | File |
|---|---|---|---|---|
| ① | `order.created` | order | payment | `OrderService.createOrder` → `PaymentListener.onOrderCreated` |
| ② | `payment.completed` | payment | product | `PaymentListener` → `InventoryListener.onPaymentCompleted` |
| ② | `payment.failed` | payment | order | → `OrderSagaListener.onPaymentFailed` |
| ③ | `inventory.reserved` | product | order | → `OrderSagaListener.onInventoryReserved` |
| ③ | `inventory.failed` | product | order | → `OrderSagaListener.onInventoryFailed` |
| Bù trừ | `payment.refund` | order | payment | → `PaymentListener.onRefund` |
| Kết | `order.confirmed` / `order.cancelled` | order | notification | → `NotificationListener` |

## 4. Hai kịch bản để tự kiểm chứng

### ✅ Kịch bản THÀNH CÔNG
Đặt sản phẩm **còn hàng** (vd "Laptop Dell XPS 13"):
```
order.created → payment.completed → inventory.reserved → order.confirmed → 📧
```
Đơn kết thúc ở trạng thái **CONFIRMED**. Kho giảm đi.

### ❌ Kịch bản THẤT BẠI + BÙ TRỪ
Đặt sản phẩm **hết hàng** ("Tai nghe Sony WH-1000XM5", stock=0):
```
order.created → payment.completed → inventory.FAILED
   → order CANCELLED
   → payment.refund (payment-service đánh dấu REFUNDED)  ◄── BÙ TRỪ
   → order.cancelled → 📧 "đơn bị huỷ, đã hoàn tiền"
```
Đây chính là điểm cốt lõi: tiền đã trừ ở bước ② được **hoàn lại** vì bước ③ thất bại — thứ mà transaction ACID làm tự động trong monolith, còn ở đây ta phải làm thủ công bằng compensation.

## 5. Các khái niệm Saga rút ra

- **Eventual consistency (nhất quán sau cùng):** đơn hàng không "đúng" ngay lập tức; nó trải qua `PENDING` rồi mới `CONFIRMED`/`CANCELLED`. Hệ thống nhất quán *sau một khoảng thời gian*, không tức thì.
- **Compensation (bù trừ):** thay cho rollback. Mỗi bước "tiến" có một bước "lùi" tương ứng (trừ tiền ↔ hoàn tiền).
- **Idempotency (bất biến khi lặp):** Kafka có thể gửi 1 message ≥ 1 lần. `PaymentListener` kiểm tra "đơn đã có payment chưa" để không trừ tiền 2 lần. Mọi consumer xử lý tiền/kho **bắt buộc** phải idempotent.
- **Choreography vs Orchestration:** ShopMicro dùng *choreography* (các service tự phối hợp qua event). Cách còn lại là *orchestration* (một service "nhạc trưởng" ra lệnh từng bước) — dễ theo dõi hơn nhưng tập trung hoá hơn.

➡️ Tiếp theo: [04 — Hướng dẫn chạy & kiểm thử](04-huong-dan-chay.md)
