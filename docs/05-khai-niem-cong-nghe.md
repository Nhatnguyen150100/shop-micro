# 05 — Từ điển khái niệm & công nghệ

Giải thích ngắn gọn các thuật ngữ và công nghệ dùng trong ShopMicro, kèm "ở đâu trong dự án".

## A. Khái niệm kiến trúc

| Thuật ngữ | Giải thích | Trong ShopMicro |
|---|---|---|
| **Microservice** | Ứng dụng nhỏ, độc lập, một nghiệp vụ | auth, user, product, order, payment, notification |
| **Bounded Context** | Ranh giới nghiệp vụ rõ ràng để chia service | mỗi service = 1 context |
| **API Gateway** | Cổng vào duy nhất | api-gateway |
| **Service Discovery** | Cơ chế service tìm nhau qua tên | Eureka |
| **Config tập trung** | Cấu hình một nơi cho mọi service | config-server |
| **Database per Service** | Mỗi service 1 DB, không chia sẻ | 5 database Postgres riêng |
| **Saga** | Giao dịch phân tán qua chuỗi bước + bù trừ | luồng đặt hàng |
| **Compensation** | Hành động hoàn tác khi bước sau lỗi | hoàn tiền (`payment.refund`) |
| **Eventual Consistency** | Nhất quán sau cùng, không tức thì | đơn PENDING → CONFIRMED/CANCELLED |
| **Idempotency** | Xử lý lặp không gây hậu quả kép | check payment đã tồn tại |
| **Circuit Breaker** | Ngắt mạch khi service đích lỗi | Resilience4j ở order-service |
| **CQRS / Event Sourcing** | (nâng cao) tách đọc/ghi, lưu chuỗi sự kiện | *chưa dùng — hướng mở rộng* |

## B. Công nghệ & vai trò

| Công nghệ | Nhóm | Vai trò trong dự án |
|---|---|---|
| **Spring Boot 3.5** | Core | Khung ứng dụng cho mọi service |
| **Spring Cloud 2025** | Core | Bộ công cụ microservices (gateway, eureka, config, openfeign) |
| **Java 21** | Core | Ngôn ngữ (dùng record cho event/DTO) |
| **Spring Cloud Gateway** | Edge | Routing, JWT filter, rate limit (reactive) |
| **Netflix Eureka** | Platform | Service discovery |
| **Spring Cloud Config** | Platform | Cấu hình tập trung |
| **OpenFeign** | Giao tiếp sync | Gọi REST liên service kiểu khai báo |
| **Resilience4j** | Chịu lỗi | Circuit breaker, timeout, fallback |
| **Apache Kafka** | Giao tiếp async | Event bus cho Saga |
| **PostgreSQL** | Data | DB cho từng service |
| **Redis** | Data | Rate limit tại Gateway (token bucket) |
| **JWT (jjwt)** | Bảo mật | Token xác thực; auth ký, gateway verify |
| **BCrypt** | Bảo mật | Mã hoá mật khẩu |
| **Micrometer + Prometheus** | Observability | Thu thập metrics |
| **Grafana** | Observability | Dashboard số liệu |
| **OpenTelemetry + Jaeger** | Observability | Distributed tracing |
| **SpringDoc OpenAPI** | DevEx | Swagger UI cho từng service |
| **Docker + Compose** | Vận hành | Đóng gói & chạy toàn hệ thống |
| **Lombok** | DevEx | Giảm code lặp (getter/builder...) |

## C. Vì sao chọn những công nghệ này?

- **Kafka thay vì RabbitMQ?** Cả hai đều tốt. Kafka mạnh về throughput cao, lưu lại lịch sử sự kiện (replay), phù hợp event-driven/streaming — phổ biến nhất hiện nay cho microservices quy mô lớn.
- **Eureka thay vì Consul/K8s DNS?** Eureka đơn giản, gắn liền hệ sinh thái Spring, dễ học. Production trên Kubernetes thường dùng luôn DNS của K8s (bỏ Eureka).
- **OpenFeign thay vì RestTemplate/WebClient?** Feign khai báo gọn (chỉ cần interface), tích hợp sẵn load-balancing + circuit breaker.
- **Jaeger thay vì Zipkin?** Tương đương; Jaeger hỗ trợ OTLP tốt và UI trực quan.

## D. Hướng mở rộng (khi đã nắm vững)

1. **Flyway** thay cho `ddl-auto=update` — quản lý schema có version (production bắt buộc).
2. **Kubernetes + Helm** thay Docker Compose — autoscaling, self-healing.
3. **Keycloak** thay JWT tự code — Identity Provider chuẩn OAuth2/OIDC.
4. **API Composition / BFF** — tổng hợp dữ liệu nhiều service cho 1 màn hình.
5. **Outbox Pattern** — đảm bảo "lưu DB và phát event" là nguyên tử (tránh mất event).
6. **Schema Registry (Avro/Protobuf)** — quản lý phiên bản hợp đồng sự kiện thay vì share common-lib.
7. **Service Mesh (Istio/Linkerd)** — mTLS, traffic shaping ở tầng hạ tầng.
8. **CI/CD (GitHub Actions + ArgoCD)** — build/test/deploy tự động theo GitOps.

## E. Bản đồ "đến từ boilerplate gốc"

| Trong ShopMicro | Kế thừa từ boilerplate gốc |
|---|---|
| `common-lib` ResponseBuilder/BaseEntity/Exception | `common/response`, `common/entities`, `exceptions` |
| `auth-service` (JWT, BCrypt, register/login) | `modules/auth`, `JwtService` |
| `user-service` | `modules/user` |
| Rate limit ở Gateway | `RateLimitFilter` (Bucket4j) → chuyển thành Redis rate limiter |
| Redis, Micrometer/Prometheus | đã có sẵn trong boilerplate |

Hết. Chúc bạn học vui! 🚀
