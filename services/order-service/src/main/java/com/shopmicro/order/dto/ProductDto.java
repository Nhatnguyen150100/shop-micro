package com.shopmicro.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Bản sao tối giản dữ liệu sản phẩm mà order-service cần khi gọi product-service. */
public record ProductDto(
    UUID id,
    String name,
    BigDecimal price,
    int stock) {
}
