# ShopMicro — Hệ thống Microservices E-commerce (dự án học tập)

Một hệ thống **microservices hoàn chỉnh** xây dựng bằng **Spring Boot 3.5 + Spring Cloud 2025 + Java 21**, áp dụng các công nghệ phổ biến và mạnh mẽ nhất hiện nay. Dự án được thiết kế để **học** kiến trúc microservices qua một ứng dụng thực tế (đặt hàng e-commerce), kế thừa ý tưởng từ Spring Boot boilerplate gốc.

> 📚 Tài liệu chi tiết nằm trong thư mục [`docs/`](docs/). Nên đọc theo thứ tự 01 → 05.

---

## 🧩 Các thành phần

| Thành phần | Cổng | Vai trò |
|---|---|---|
| **discovery-server** (Eureka) | 8761 | "Danh bạ" — service đăng ký & tìm nhau qua tên |
| **config-server** | 8888 | Cấu hình tập trung |
| **api-gateway** | 8080 | Cổng vào duy nhất: routing, xác thực JWT, rate limit |
| **auth-service** | 8081 | Đăng ký, đăng nhập, cấp JWT |
| **user-service** | 8082 | Hồ sơ người dùng |
| **product-service** | 8083 | Catalog + tồn kho (Kafka) |
| **order-service** | 8084 | Đặt hàng; tham gia Saga (nhận lệnh confirm/cancel) |
| **payment-service** | 8085 | Thanh toán + hoàn tiền (Kafka command/reply) |
| **notification-service** | 8086 | Gửi email khi đơn hoàn tất/huỷ (Kafka) |
| **saga-orchestrator-service** | 8087 | **Nhạc trưởng** điều phối Saga đặt hàng (orchestration) |
| **common-lib** | — | Thư viện dùng chung (response, exception, event) |

**Hạ tầng:** PostgreSQL (mỗi service 1 DB), Redis, Apache Kafka, Jaeger (tracing), Prometheus + Grafana (metrics).

---

## 🗺️ Sơ đồ kiến trúc

```
                     ┌─────────────┐
   Client ─────────► │ API Gateway │  :8080  (JWT verify, rate limit, routing)
                     └──────┬──────┘
       ┌──────────┬─────────┼─────────┬──────────┐
       ▼          ▼         ▼         ▼          ▼
   auth(8081) user(8082) product(8083) order(8084) payment(8085)
       │          │         │          │          │
    authDB     userDB    productDB   orderDB    paymentDB   (PostgreSQL)
                          │          │          │
                          └────── Apache Kafka ─┘──► notification(8086) ──► 📧
       (mọi service đăng ký vào Eureka :8761, lấy config từ :8888,
        đẩy trace về Jaeger :16686, metrics về Prometheus :9090 / Grafana :3000)
```

---

## 🚀 Chạy nhanh (Docker — khuyến nghị)

Yêu cầu: **Docker Desktop**. Một lệnh duy nhất:

```bash
cd shopmicro
docker compose up -d --build
```

> ⏳ Lần đầu build sẽ lâu (Maven tải dependency cho 9 service). Các lần sau nhanh hơn.

Kiểm tra mọi service đã `UP` trong Eureka: http://localhost:8761

### Các đường dẫn hữu ích
| Giao diện | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Eureka (Service Discovery) | http://localhost:8761 |
| Jaeger (Distributed Tracing) | http://localhost:16686 |
| Prometheus | http://localhost:9090 |
| Grafana (admin/admin) | http://localhost:3000 |
| Swagger auth-service | http://localhost:8081/swagger-ui.html |

---

## 🧪 Thử luồng đặt hàng (Saga)

Mở file [`api-requests.http`](api-requests.http) và chạy lần lượt, hoặc dùng curl:

```bash
# 1. Đăng ký
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@shopmicro.com","password":"123456","fullName":"Demo"}'

# 2. Đăng nhập -> lấy accessToken
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@shopmicro.com","password":"123456"}'

# 3. Xem sản phẩm (công khai) -> copy 1 productId còn hàng
curl http://localhost:8080/api/products

# 4. Đặt hàng (thay TOKEN và PRODUCT_ID)
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":"PRODUCT_ID","quantity":1}'

# 5. Xem đơn -> trạng thái chuyển PENDING -> CONFIRMED (hoặc CANCELLED nếu hết hàng)
curl http://localhost:8080/api/orders -H "Authorization: Bearer TOKEN"
```

👉 Xem log của `notification-service` để thấy "email" được gửi:
```bash
docker logs -f sm-notification
```

---

## 💻 Chạy bằng Maven (không Docker)

Cần: **JDK 21**, **Maven**, và Postgres/Redis/Kafka chạy sẵn (có thể chỉ bật phần hạ tầng bằng Docker).

```bash
# Bật riêng hạ tầng
docker compose up -d postgres redis kafka jaeger prometheus grafana

# Build toàn bộ
mvn clean install -DskipTests

# Chạy từng service (mỗi cái 1 terminal), theo thứ tự:
mvn -pl infrastructure/discovery-server spring-boot:run
mvn -pl infrastructure/config-server   spring-boot:run
mvn -pl services/auth-service          spring-boot:run
# ... product, order, payment, notification, user, api-gateway
```

---

## 📖 Tài liệu

1. [Kiến trúc tổng quan](docs/01-kien-truc-tong-quan.md)
2. [Giải thích chi tiết từng thành phần](docs/02-cac-thanh-phan.md)
3. [Luồng Saga đặt hàng (quan trọng nhất)](docs/03-luong-saga-dat-hang.md)
4. [Hướng dẫn chạy & kiểm thử](docs/04-huong-dan-chay.md)
5. [Từ điển khái niệm & công nghệ](docs/05-khai-niem-cong-nghe.md)
