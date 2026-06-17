package com.shopmicro.common.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Sự kiện: một đơn hàng vừa được tạo (trạng thái PENDING).
 * payment-service lắng nghe sự kiện này để bắt đầu thanh toán.
 */
public record OrderCreatedEvent(
    UUID orderId,
    UUID userId,
    String email,
    UUID productId,
    int quantity,
    BigDecimal amount) {
}
