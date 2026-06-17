package com.shopmicro.common.event;

import java.util.UUID;

/**
 * Sự kiện trạng thái cuối của đơn hàng (CONFIRMED/CANCELLED).
 * notification-service lắng nghe để gửi email cho khách.
 */
public record OrderStatusEvent(
    UUID orderId,
    UUID userId,
    String email,
    String status,
    String reason) {
}
