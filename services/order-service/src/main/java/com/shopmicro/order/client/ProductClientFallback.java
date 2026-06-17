package com.shopmicro.order.client;

import com.shopmicro.common.exception.BadRequestException;
import com.shopmicro.order.dto.ApiResult;
import com.shopmicro.order.dto.ProductDto;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Khi product-service không phản hồi (timeout/lỗi), circuit breaker mở và gọi vào đây
 * thay vì để lỗi lan ra. Ở đây chọn cách "fail fast" với thông báo rõ ràng.
 */
@Component
public class ProductClientFallback implements ProductClient {

  @Override
  public ApiResult<ProductDto> getById(UUID id) {
    throw new BadRequestException("product-service đang không khả dụng, vui lòng thử lại sau");
  }
}
