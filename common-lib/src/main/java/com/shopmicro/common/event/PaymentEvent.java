package com.shopmicro.common.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Sự kiện kết quả thanh toán (hoặc lệnh hoàn tiền).
 * Mang theo đủ dữ liệu để bước tiếp theo của Saga xử lý mà không cần gọi ngược lại.
 */
public record PaymentEvent(
    UUID orderId,
    UUID userId,
    String email,
    UUID productId,
    int quantity,
    BigDecimal amount,
    String status) {
}
