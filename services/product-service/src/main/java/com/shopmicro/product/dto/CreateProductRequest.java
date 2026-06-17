package com.shopmicro.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateProductRequest(
    @NotBlank(message = "Tên sản phẩm không được để trống") String name,
    String description,
    @NotNull @Min(value = 0, message = "Giá phải >= 0") BigDecimal price,
    @Min(value = 0, message = "Tồn kho phải >= 0") int stock) {
}
