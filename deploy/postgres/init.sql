-- Mỗi service một database riêng (nguyên tắc "database per service").
-- Postgres chạy file này 1 lần khi khởi tạo volume lần đầu.
CREATE DATABASE authdb;
CREATE DATABASE userdb;
CREATE DATABASE productdb;
CREATE DATABASE orderdb;
CREATE DATABASE paymentdb;
CREATE DATABASE sagadb;
