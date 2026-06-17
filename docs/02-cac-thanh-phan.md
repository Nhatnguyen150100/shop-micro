# 02 — Giải thích chi tiết từng thành phần

Tài liệu này đi qua từng module, giải thích **nó làm gì, code nằm ở đâu, vì sao thiết kế như vậy**.

---

## common-lib (thư viện dùng chung)

Không phải service chạy được, mà là **jar dùng chung** cho mọi service. Tránh lặp code.

| File | Vai trò |
|---|---|
| `response/BaseResponse`, `ResponseBuilder` | Định dạng JSON trả về thống nhất cho mọi service |
| `entity/BaseEntity` | Khoá chính UUID + audit (createdAt/updatedAt) tự động |
| `exception/*` + `GlobalExceptionHandler` | Bắt lỗi toàn cục, trả về đúng mã HTTP |
| `event/*` (OrderCreatedEvent, PaymentEvent, InventoryEvent, OrderStatusEvent) | **Hợp đồng sự kiện Kafka** — dùng chung giữa các service |
| `event/EventTopics` | Hằng số tên topic (tránh gõ sai tên) |
| `security/AuthHeaders` | Tên các header `X-User-*` Gateway gắn vào |

> ⚖️ **Đánh đổi:** chia sẻ event qua common-lib rất tiện cho việc học, nhưng trong thực tế lớn nó tạo "khớp nối" (coupling) giữa các service. Giải pháp nâng cao: schema registry (Avro/Protobuf). Với ShopMicro, common-lib là lựa chọn hợp lý.

---

## discovery-server (Eureka) — cổng 8761

**Vấn đề giải quyết:** trong microservices, địa chỉ IP của service thay đổi liên tục (scale, restart, container). Không thể hard-code IP.

**Cách hoạt động:**
1. Mỗi service khi khởi động gửi "tôi là PRODUCT-SERVICE, ở địa chỉ X" lên Eureka.
2. Khi `order-service` muốn gọi `product-service`, nó hỏi Eureka "PRODUCT-SERVICE ở đâu?" và nhận về địa chỉ.
3. Eureka cũng làm **load balancing**: nếu có 3 instance product-service, request được chia đều.

Code: chỉ cần annotation `@EnableEurekaServer`. Mỗi client cần dependency `eureka-client`.

---

## config-server — cổng 8888

**Vấn đề:** 9 service, mỗi cái có config. Sửa URL Kafka mà phải sửa 9 file rồi build lại 9 lần thì rất khổ.

**Cách hoạt động:** đặt config dùng chung ở một nơi (`config/application.yml`). Service khởi động sẽ "import" config từ đây (qua `spring.config.import=optional:configserver:...`).

ShopMicro dùng chế độ **native** (đọc file YAML trong classpath). Thực tế production thường trỏ vào **Git repo** để có version control cho config.

> `optional:` nghĩa là nếu config-server chết, service vẫn chạy được bằng config cục bộ — tăng khả năng chịu lỗi.

---

## api-gateway — cổng 8080

Trái tim của lớp Edge. Trách nhiệm:

### 1. Routing (định tuyến)
Định nghĩa trong `application.yml`: `/api/auth/**` → `AUTH-SERVICE`, `/api/orders/**` → `ORDER-SERVICE`... `uri: lb://AUTH-SERVICE` nghĩa là "load-balance qua Eureka theo tên".

### 2. Xác thực JWT tập trung
`filter/JwtAuthenticationFilter.java` (một `GlobalFilter`):
- Route công khai (auth, swagger, GET sản phẩm) → cho qua.
- Route cần bảo vệ → verify chữ ký JWT. Hợp lệ thì gắn `X-User-*` rồi chuyển tiếp; sai thì trả 401 ngay (request không bao giờ chạm service).

### 3. Rate limiting
`config/RateLimitConfig.java` + filter `RequestRateLimiter` trong route auth: giới hạn 5 req/giây/IP bằng **Redis** (thuật toán token bucket). Đây là phiên bản phân tán của `RateLimitFilter` trong boilerplate gốc.

> Gateway dùng **WebFlux (reactive)** nên code filter trả về `Mono<Void>` — khác với servlet trong các service nghiệp vụ.

---

## auth-service — cổng 8081

Kế thừa trực tiếp logic auth từ boilerplate gốc, rút gọn cho mục tiêu học.

- `entity/User` — kế thừa `BaseEntity`, lưu email/password (BCrypt)/role.
- `service/AuthService` — `register` (kiểm tra trùng email, mã hoá mật khẩu), `login` (so khớp mật khẩu).
- `service/JwtService` — sinh JWT chứa claim `id`, `role`, `email`. **Khoá ký phải trùng Gateway.**
- `controller/AuthController` — `POST /api/auth/register`, `POST /api/auth/login`.

Dùng `spring-security-crypto` (chỉ lấy BCrypt) thay vì cả Spring Security filter chain — vì việc bảo vệ endpoint đã do Gateway lo.

---

## user-service — cổng 8082

Minh hoạ rõ nhất mô hình "Gateway-centric": `controller/UserController` **không đọc JWT**, mà đọc header `X-User-Id` / `X-User-Email` do Gateway gắn vào (qua `@RequestHeader(AuthHeaders.USER_ID)`).

- `getOrCreate`: lần đầu user gọi `/api/users/me`, hồ sơ được tạo tự động (lazy provisioning) từ thông tin định danh.
- Lưu ý: user-service **không lưu password** — đó là việc của auth-service. Mỗi service chỉ giữ dữ liệu thuộc về mình.

---

## product-service — cổng 8083

Hai vai trò:
1. **REST đồng bộ:** `GET /api/products` (công khai), `POST /api/products` (cần đăng nhập). order-service gọi vào đây để lấy giá.
2. **Kafka (Saga):** `messaging/InventoryListener` lắng nghe `payment.completed` → trừ kho → phát `inventory.reserved` hoặc `inventory.failed`.

`config/DataSeeder` nạp sẵn 3 sản phẩm mẫu (trong đó "Tai nghe Sony" có `stock=0` để test luồng thất bại).

---

## order-service — cổng 8084 (phức tạp & quan trọng nhất)

Vừa gọi đồng bộ, vừa điều phối Saga bất đồng bộ:

- `client/ProductClient` (**OpenFeign**): gọi product-service kiểu khai báo, có `ProductClientFallback` (**Resilience4j circuit breaker**) khi product-service lỗi.
- `service/OrderService.createOrder`: lấy giá (đồng bộ) → lưu đơn `PENDING` → phát `order.created`.
- `messaging/OrderSagaListener`: lắng nghe kết quả các bước, quyết định `CONFIRMED`/`CANCELLED` và kích hoạt **bù trừ** (hoàn tiền) khi cần.

Chi tiết luồng: [doc 03](03-luong-saga-dat-hang.md).

---

## payment-service — cổng 8085

- `messaging/PaymentListener.onOrderCreated`: lắng nghe `order.created` → "thanh toán" (mô phỏng: thành công nếu số tiền ≤ hạn mức) → phát `payment.completed`/`payment.failed`.
- `onRefund`: lắng nghe `payment.refund` → đánh dấu `REFUNDED` (bước **bù trừ** của Saga).
- **Idempotency:** kiểm tra đơn đã có payment chưa, tránh trừ tiền 2 lần khi Kafka gửi lặp message.

---

## notification-service — cổng 8086

Consumer Kafka thuần (không DB). Lắng nghe `order.confirmed` / `order.cancelled` → "gửi email" (ghi log).

> Đây là minh hoạ đẹp nhất cho giá trị event-driven: notification-service **không biết gì** về order/payment/product, chỉ phản ứng với sự kiện. Muốn thêm SMS/push? Thêm 1 consumer mới, **không sửa** service nào khác.

➡️ Tiếp theo: [03 — Luồng Saga đặt hàng](03-luong-saga-dat-hang.md)
