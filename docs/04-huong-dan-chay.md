# 04 — Hướng dẫn chạy & kiểm thử

## 1. Yêu cầu

| Cách chạy | Cần cài |
|---|---|
| **Docker (khuyến nghị)** | Docker Desktop (đã bật) |
| **Maven thủ công** | JDK 21, Maven 3.9+, và hạ tầng (Postgres/Redis/Kafka) |

## 2. Chạy bằng Docker (toàn bộ hệ thống)

```bash
cd shopmicro
docker compose up -d --build
```

Theo dõi quá trình khởi động:
```bash
docker compose ps          # xem trạng thái container
docker compose logs -f api-gateway
```

> ⏳ **Lần đầu rất lâu** vì mỗi service build Maven riêng (tải dependency). Kiên nhẫn ~5–15 phút tuỳ máy/mạng. Lần sau dùng cache nên nhanh.

**Xác nhận thành công:** mở http://localhost:8761 — tất cả 7 service (gateway, auth, user, product, order, payment, notification) phải hiện trạng thái `UP`.

### Dừng / xoá
```bash
docker compose down          # dừng
docker compose down -v        # dừng + xoá dữ liệu (postgres volume)
```

## 3. Kiểm thử luồng đặt hàng (end-to-end)

Dùng file [`api-requests.http`](../api-requests.http) (VS Code REST Client / IntelliJ) hoặc curl. Trình tự:

1. **Đăng ký** `POST /api/auth/register`
2. **Đăng nhập** `POST /api/auth/login` → copy `accessToken`
3. **Xem sản phẩm** `GET /api/products` → copy `id` của:
   - 1 sản phẩm còn hàng (Laptop / iPhone)
   - sản phẩm hết hàng (Tai nghe Sony, stock=0)
4. **Đặt hàng thành công** `POST /api/orders` với productId còn hàng
5. **Đặt hàng thất bại** `POST /api/orders` với productId hết hàng
6. **Xem đơn** `GET /api/orders` → quan sát trạng thái

### Quan sát Saga chạy
Mở log song song để thấy sự kiện truyền qua các service:
```bash
docker compose logs -f order-service payment-service product-service notification-service
```
Bạn sẽ thấy chuỗi log:
```
[order]   Tạo đơn ... (PENDING) và phát order.created
[payment] Đơn ... -> COMPLETED (phát payment.completed)
[product] Trừ kho THÀNH CÔNG ... / THẤT BẠI ...
[order]   Đơn ... -> CONFIRMED  (hoặc CANCELLED + payment.refund)
[notification] 📧 GỬI EMAIL -> ...
```

## 4. Khám phá hạ tầng

| Việc muốn xem | Vào đâu |
|---|---|
| Service nào đang sống | Eureka — http://localhost:8761 |
| Một request đi qua những service nào (trace) | Jaeger — http://localhost:16686 (chọn service `api-gateway`) |
| Số liệu CPU/request/latency | Prometheus http://localhost:9090 hoặc Grafana http://localhost:3000 (admin/admin) |
| Thử API trực quan | Swagger từng service, vd http://localhost:8081/swagger-ui.html |

### Xem Distributed Tracing trên Jaeger
1. Tạo vài request đặt hàng.
2. Mở Jaeger UI → chọn Service = `order-service` → Find Traces.
3. Click một trace: thấy span đi từ gateway → order → (feign) product, và thời gian từng chặng. Đây là cách debug "chậm ở đâu" trong hệ phân tán.

## 5. Chạy bằng Maven (không Docker app, chỉ hạ tầng Docker)

```bash
# 1. Bật hạ tầng
docker compose up -d postgres redis kafka jaeger prometheus grafana

# 2. Build toàn bộ (cài common-lib vào local maven repo)
mvn clean install -DskipTests

# 3. Chạy từng service theo thứ tự (mỗi cái 1 cửa sổ terminal)
mvn -pl infrastructure/discovery-server spring-boot:run
mvn -pl infrastructure/config-server   spring-boot:run
mvn -pl services/auth-service          spring-boot:run
mvn -pl services/user-service          spring-boot:run
mvn -pl services/product-service       spring-boot:run
mvn -pl services/order-service         spring-boot:run
mvn -pl services/payment-service       spring-boot:run
mvn -pl services/notification-service  spring-boot:run
mvn -pl infrastructure/api-gateway     spring-boot:run
```
Khi chạy ngoài Docker, các biến môi trường mặc định trỏ về `localhost` (xem giá trị mặc định trong mỗi `application.yml`), nên không cần set gì thêm.

## 6. Xử lý sự cố thường gặp

| Triệu chứng | Nguyên nhân & cách xử lý |
|---|---|
| Gateway trả 401 dù đã đăng nhập | `jwt.secret` ở gateway và auth-service không trùng nhau |
| Service không lên `UP` trên Eureka | Eureka chưa sẵn sàng khi service khởi động — đợi thêm, service sẽ tự retry |
| Đơn cứ ở `PENDING` mãi | Kafka chưa sẵn sàng, hoặc consumer chưa nhận; xem log payment/product-service |
| Đặt 2 lần bị trừ tiền/kho 2 lần | Kiểm tra logic idempotency trong listener |
| Build Docker lỗi out-of-memory | Tăng RAM cho Docker Desktop (khuyến nghị ≥ 6GB) |
| Maven báo không tìm thấy artifact gateway | Tuỳ phiên bản Spring Cloud, starter gateway có thể đổi tên thành `spring-cloud-starter-gateway-server-webflux`. Đổi trong `infrastructure/api-gateway/pom.xml` nếu cần |

> 📌 **Ghi chú tương thích phiên bản:** dự án dùng **Spring Boot 3.5.0** + **Spring Cloud 2025.0.0** (đây là cặp tương thích chuẩn). Nếu bạn nâng/hạ một bên, hãy đối chiếu bảng tương thích chính thức của Spring Cloud, vì tên một số artifact (đặc biệt Gateway) thay đổi giữa các phiên bản.

➡️ Tiếp theo: [05 — Từ điển khái niệm & công nghệ](05-khai-niem-cong-nghe.md)
