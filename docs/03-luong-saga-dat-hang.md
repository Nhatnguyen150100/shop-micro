# 03 — Luồng Saga đặt hàng (phần quan trọng nhất)

Đây là phần tinh tuý nhất để hiểu microservices. Hãy đọc kỹ.

## 1. Vấn đề: giao dịch phân tán

Đặt một đơn hàng cần 3 việc, ở 3 service + 3 database khác nhau:
1. Tạo đơn (order-service / orderdb)
2. Trừ tiền (payment-service / paymentdb)
3. Trừ kho (product-service / productdb)

Trong monolith, ta bọc cả 3 trong **một transaction ACID** — lỗi thì rollback hết. Nhưng ở microservices, **không có transaction xuyên service/database**. Nếu trừ tiền xong mà trừ kho thất bại (hết hàng) thì sao? → Phải **hoàn tiền**.

Giải pháp: **Saga Pattern** — chuỗi các giao dịch cục bộ, mỗi bước có một **hành động bù trừ (compensation)** để hoàn tác khi bước sau thất bại.

## 2. Choreography vs Orchestration

Có hai cách triển khai saga:

- **Choreography:** các service tự phối hợp qua event ("tôi nghe event A thì làm việc rồi phát event B"). Không có trung tâm — phi tập trung, nhưng logic điều phối **rải rác** khắp các service và **không nơi nào** nắm toàn cảnh trạng thái.
- **Orchestration:** có một **nhạc trưởng (orchestrator)** ra lệnh từng bước. Orchestrator gửi COMMAND ("hãy trừ tiền") cho participant, nhận REPLY ("xong/lỗi"), rồi quyết định bước kế tiếp hoặc bù trừ. Logic **tập trung một chỗ**, dễ theo dõi/debug, có **saga log** để biết mỗi đơn đang ở đâu.

> **ShopMicro dùng ORCHESTRATION.** Nhạc trưởng là `saga-orchestrator-service` (port 8087, DB `sagadb`). Các service payment/product/order trở thành "tay chân" — chỉ thực thi lệnh và trả kết quả, **không biết bước trước/sau**.

## 3. Sơ đồ luồng đầy đủ

```
        ┌──────────────┐
Client ►│ order-service│ tạo đơn PENDING, lưu orderdb, phát "order.created"
        └──────┬───────┘
               ▼  [ Kafka: order.created ]
        ┌─────────────────────────┐
        │ saga-orchestrator-service│  tạo saga_state, BẮT ĐẦU điều phối
        └──────────┬──────────────┘
                   │ ① cmd.process-payment
                   ▼
            payment-service ──reply──► (COMPLETED / FAILED)
                   │
        ┌──────────┴─────────────────────┐
        ▼ reply OK                        ▼ reply FAIL
  ② cmd.reserve-stock               cmd.cancel-order
        ▼                                 ▼  (chưa trừ gì → không bù trừ)
  product-service ──reply──►          order-service → CANCELLED
   (RESERVED / FAILED)
        │
   ┌────┴──────────────┐
   ▼ reply OK           ▼ reply FAIL
 cmd.confirm-order   ③ cmd.refund-payment  ◄── BÙ TRỪ (tiền đã trừ ở bước ①)
   ▼                    ▼
 order-service →     payment-service (REFUNDED) ──reply──►
 CONFIRMED           orchestrator → cmd.cancel-order → order-service → CANCELLED
        │                    │
        └──► order.confirmed / order.cancelled ──► notification-service ──► 📧
```

Mọi REPLY (từ payment/product) đều gửi về **một topic duy nhất** `saga.reply`; orchestrator nghe topic này và dựa vào `saga_state.step` để quyết định.

## 4. Đối chiếu lệnh/sự kiện ↔ code

| Bước | Topic Kafka | Người gửi | Người nhận | File |
|---|---|---|---|---|
| Khởi động | `order.created` | order | orchestrator | `OrderService.createOrder` → `SagaTriggerListener` → `OrderSagaOrchestrator.start` |
| ① lệnh | `saga.cmd.process-payment` | orchestrator | payment | → `PaymentListener.onProcessPayment` |
| ① kết quả | `saga.reply` | payment | orchestrator | → `SagaReplyListener` → `onPaymentReply` |
| ② lệnh | `saga.cmd.reserve-stock` | orchestrator | product | → `InventoryListener.onReserveStock` |
| ② kết quả | `saga.reply` | product | orchestrator | → `onStockReply` |
| Bù trừ | `saga.cmd.refund-payment` | orchestrator | payment | → `PaymentListener.onRefund` |
| Bù trừ kq | `saga.reply` | payment | orchestrator | → `onRefundReply` |
| Chốt đơn | `saga.cmd.confirm-order` / `saga.cmd.cancel-order` | orchestrator | order | → `OrderCommandListener` |
| Kết | `order.confirmed` / `order.cancelled` | order | notification | → `NotificationListener` |

Bảng quyết định của orchestrator (trong `OrderSagaOrchestrator`):

| step hiện tại | reply | hành động |
|---|---|---|
| `PAYMENT_PENDING` | payment OK | → `STOCK_PENDING`, gửi reserve-stock |
| `PAYMENT_PENDING` | payment FAIL | → `FAILED`, gửi cancel-order |
| `STOCK_PENDING` | stock OK | → `COMPLETED`, gửi confirm-order |
| `STOCK_PENDING` | stock FAIL | → `COMPENSATING`, gửi refund-payment |
| `COMPENSATING` | refund OK | → `CANCELLED`, gửi cancel-order |

## 5. Hai kịch bản để tự kiểm chứng

### ✅ Kịch bản THÀNH CÔNG
Đặt sản phẩm **còn hàng** (vd "Laptop Dell XPS 13"):
```
order.created → process-payment(OK) → reserve-stock(OK) → confirm-order → 📧
saga_state: STARTED → PAYMENT_PENDING → STOCK_PENDING → COMPLETED
```
Đơn kết thúc ở trạng thái **CONFIRMED**. Kho giảm đi.

### ❌ Kịch bản THẤT BẠI + BÙ TRỪ
Đặt sản phẩm **hết hàng** ("Tai nghe Sony WH-1000XM5", stock=0):
```
order.created → process-payment(OK) → reserve-stock(FAIL)
   → refund-payment (payment-service đánh dấu REFUNDED)  ◄── BÙ TRỪ
   → cancel-order → order.cancelled → 📧 "đơn bị huỷ, đã hoàn tiền"
saga_state: ... → STOCK_PENDING → COMPENSATING → CANCELLED
```
Tiền đã trừ ở bước ① được **hoàn lại** vì bước ② thất bại — thứ mà transaction ACID làm tự động trong monolith, còn ở đây orchestrator điều phối thủ công bằng compensation.

## 6. Các khái niệm Saga rút ra

- **Eventual consistency (nhất quán sau cùng):** đơn trải qua `PENDING` rồi mới `CONFIRMED`/`CANCELLED`. Hệ thống nhất quán *sau một khoảng thời gian*, không tức thì.
- **Compensation (bù trừ):** thay cho rollback. Mỗi bước "tiến" có một bước "lùi" (trừ tiền ↔ hoàn tiền, trừ kho ↔ hoàn kho).
- **Saga log:** bảng `saga_state` lưu bước hiện tại của từng đơn → quan sát & khôi phục được. Đây là lợi thế lớn của orchestration so với choreography.
- **Idempotency (bất biến khi lặp):** Kafka có thể gửi 1 message ≥ 1 lần.
  - `PaymentListener` kiểm tra "đơn đã có payment chưa" để không trừ tiền 2 lần.
  - Orchestrator chỉ xử lý reply khi `saga_state.step` đúng bước kỳ vọng (`expect(...)`), cộng `@Version` (optimistic lock) chặn cập nhật đua → reply trùng/lạc bước bị bỏ qua.
  - `OrderCommandListener` chỉ chốt đơn khi đang `PENDING`.
- **Coupling:** participant chỉ phụ thuộc vào *hợp đồng lệnh/reply* (`SagaCommand`/`SagaReply` trong common-lib), không gọi trực tiếp nhau cho luồng saga.

## 7. Transactional Outbox (chống dual-write)

**Vấn đề "dual-write":** trong một bước saga, service vừa ghi DB (vd lưu `saga_state`,
`payment`, đổi trạng thái đơn) vừa gửi message Kafka. Nếu commit DB xong nhưng gửi
Kafka lỗi (mạng/broker chết) → DB đã đổi mà message **biến mất** → saga "đứng hình".
Hai kho lưu trữ (DB + Kafka) không nằm trong cùng một transaction.

**Giải pháp đã triển khai:** thay vì gửi thẳng Kafka, mỗi bước **ghi message vào bảng
`outbox_event` trong CÙNG transaction** với dữ liệu nghiệp vụ. Một tiến trình nền
(relay) đọc các bản ghi chưa gửi và publish lên Kafka.

```
[ Transaction nghiệp vụ ]                 [ Tiến trình nền - mỗi 2s ]
  - lưu saga_state / payment / order        OutboxRelay:
  - INSERT outbox_event (cùng tx)    ──►      đọc outbox_event WHERE published=false
        (cùng commit / cùng rollback)         → kafkaTemplate.send(...).get()
                                              → đánh dấu published=true
```

Nhờ vậy DB và "ý định gửi message" **cùng commit hoặc cùng rollback** — không bao giờ
lệch nhau. Đánh đổi: giao hàng **at-least-once** (relay có thể gửi lặp nếu crash giữa
chừng) → đúng lý do vì sao mọi consumer ở đây đã được làm **idempotent**.

| Thành phần | Vai trò | File |
|---|---|---|
| `OutboxEvent` | entity bảng `outbox_event` (topic, key, eventType, payload JSON, published) | common-lib |
| `OutboxPublisher` | API nghiệp vụ gọi thay `kafkaTemplate.send` (`@Transactional(MANDATORY)`) | common-lib |
| `OutboxRelay` | `@Scheduled` quét & publish bản ghi chưa gửi, chờ Kafka xác nhận rồi mới đánh dấu | common-lib |
| `OutboxAutoConfiguration` | tự bật outbox khi service có **cả** Kafka **lẫn** JPA | common-lib |

Cơ chế bật tự động (`@ConditionalOnClass(KafkaTemplate)` + `@ConditionalOnBean(EntityManagerFactory)`)
khiến auth/user (không Kafka) và notification (không JPA) **tự động bỏ qua** outbox.
Bốn service publish event (order, payment, product, saga-orchestrator) chỉ cần khai
`@EntityScan("com.shopmicro")` để nạp entity `OutboxEvent`.

## 8. Giới hạn hiện tại (TODO nâng cao)

- ~~**Dual-write**~~ → đã xử lý bằng **Transactional Outbox** (mục 7).
- **Outbox đa-instance:** `OutboxRelay` hiện quét đơn giản; chạy nhiều instance cùng lúc
  cần khoá hàng (`SELECT ... FOR UPDATE SKIP LOCKED`) để tránh gửi trùng.
- **reserveStock chưa khoá / chưa idempotent:** đọc-ghi tồn kho không có lock → có thể
  đua khi nhiều đơn đồng thời; reserve lặp có thể trừ kho 2 lần. Nên dùng `@Version`
  hoặc `UPDATE ... WHERE stock >= qty` + bảng theo dõi đơn đã reserve.
- **Refund lỗi:** orchestrator giữ saga ở `COMPENSATING` và log để can thiệp thủ công;
  thực tế cần retry/dead-letter.

➡️ Tiếp theo: [04 — Hướng dẫn chạy & kiểm thử](04-huong-dan-chay.md)
