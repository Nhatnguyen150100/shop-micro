# 01 — Kiến trúc tổng quan

## 1. Tại sao là Microservices?

Boilerplate gốc là một **monolith**: tất cả module (auth, user, upload) nằm chung một ứng dụng, một database, deploy một lần. ShopMicro tách nó thành nhiều **service độc lập**:

| Tiêu chí | Monolith (boilerplate gốc) | Microservices (ShopMicro) |
|---|---|---|
| Triển khai | 1 lần cho cả app | Mỗi service deploy riêng |
| Database | 1 DB chung | Mỗi service 1 DB riêng |
| Scale | Scale cả khối | Scale từng service |
| Lỗi | 1 lỗi có thể sập cả app | Lỗi được cô lập từng service |
| Độ phức tạp | Thấp (trong code) | Cao (trong vận hành) |

> ⚠️ Microservices **không phải lúc nào cũng tốt hơn**. Nó đánh đổi sự đơn giản trong code lấy sự phức tạp trong vận hành (mạng, dữ liệu phân tán, theo dõi hệ thống). ShopMicro tồn tại để **học** các kỹ thuật đó.

## 2. Bản đồ thành phần

Hệ thống chia làm 3 lớp:

### Lớp Edge (cửa ngõ)
- **api-gateway**: điểm vào duy nhất. Client chỉ cần biết địa chỉ Gateway, không cần biết có bao nhiêu service phía sau.

### Lớp Platform (nền tảng điều phối)
- **discovery-server (Eureka)**: danh bạ. Mỗi service tự đăng ký tên + địa chỉ; service khác tra cứu nhau qua **tên** thay vì IP (IP thay đổi khi scale).
- **config-server**: cấu hình tập trung, đổi config không cần build lại.

### Lớp Business (nghiệp vụ)
- **auth, user, product, order, payment, notification** — mỗi service một bounded context (một mảng nghiệp vụ rõ ràng).

## 3. Hai kiểu giao tiếp giữa các service

ShopMicro minh hoạ **cả hai** kiểu giao tiếp — đây là điểm học quan trọng:

### a) Đồng bộ (Synchronous) — REST qua OpenFeign + Resilience4j
Dùng khi cần **dữ liệu trả về ngay**. Ví dụ: `order-service` gọi `product-service` để lấy giá sản phẩm khi tạo đơn.
```
order-service ──HTTP──► product-service   (chờ phản hồi)
```
Được bọc **circuit breaker**: nếu product-service treo/lỗi, mạch ngắt và trả fallback thay vì chờ vô tận.

### b) Bất đồng bộ (Asynchronous) — Event qua Apache Kafka
Dùng cho **side-effect, decoupling**. Service phát một sự kiện rồi đi tiếp, không chờ. Service khác lắng nghe và xử lý.
```
order-service ──phát "order.created"──► Kafka ──► payment-service (xử lý sau)
```
Đây là xương sống của luồng **Saga** (xem [doc 03](03-luong-saga-dat-hang.md)).

## 4. Bảo mật — mô hình "Gateway-centric"

```
Client ──(JWT)──► API Gateway ──(verify JWT 1 lần)──► gắn header X-User-* ──► service nội bộ
```

1. `auth-service` cấp JWT khi đăng nhập (ký bằng khoá bí mật `jwt.secret`).
2. `api-gateway` **verify** chữ ký JWT cho **mọi** request cần bảo vệ (dùng cùng khoá đó).
3. Nếu hợp lệ → Gateway bóc thông tin user, gắn vào header `X-User-Id`, `X-User-Email`, `X-User-Role`, rồi chuyển xuống service.
4. Service nội bộ **không verify JWT lại** — chỉ đọc header (xem `UserController.me()`). Service nội bộ không expose ra internet nên tin tưởng được Gateway.

> Khoá `jwt.secret` trong `api-gateway` và `auth-service` **phải trùng nhau** — đây là lỗi thường gặp khi mới làm.

## 5. Dữ liệu — "Database per Service"

Mỗi service sở hữu database riêng (`authdb`, `userdb`, `productdb`, `orderdb`, `paymentdb`) và **không truy cập DB của service khác**. Hệ quả:
- ✅ Service thật sự độc lập, đổi schema không ảnh hưởng service khác.
- ❌ Không thể dùng `JOIN` hay transaction ACID xuyên service → phải dùng **Saga** cho giao dịch phân tán.

## 6. Observability — "nhìn thấy" hệ thống phân tán

Khi 1 request đi qua nhiều service, debug rất khó. Ba trụ cột:
- **Metrics** (Prometheus + Grafana): hệ thống có khoẻ không?
- **Tracing** (Jaeger + OpenTelemetry): 1 request đi qua những service nào, chậm ở đâu?
- **Logging**: chuyện gì đã xảy ra (mỗi service log riêng).

Mọi service đã cấu hình sẵn cả ba (qua `micrometer` + `actuator` + OTLP).

➡️ Tiếp theo: [02 — Giải thích chi tiết từng thành phần](02-cac-thanh-phan.md)
