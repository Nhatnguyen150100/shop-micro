package com.shopmicro.common.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Sự kiện kết quả trừ kho từ product-service.
 * status = RESERVED (thành công) hoặc FAILED (hết hàng).
 */
public record InventoryEvent(
    UUID orderId,
    UUID userId,
    String email,
    UUID productId,
    int quantity,
    BigDecimal amount,
    String status) {
}
