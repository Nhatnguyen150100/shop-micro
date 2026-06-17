package com.shopmicro.order.client;

import com.shopmicro.order.dto.ApiResult;
import com.shopmicro.order.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Gọi product-service kiểu KHAI BÁO. Feign tự tra cứu địa chỉ "product-service"
 * qua Eureka và load-balance. fallback = lớp dự phòng khi service đích lỗi/treo
 * (Resilience4j circuit breaker).
 */
@FeignClient(name = "product-service", fallback = ProductClientFallback.class)
public interface ProductClient {

  @GetMapping("/api/products/{id}")
  ApiResult<ProductDto> getById(@PathVariable("id") UUID id);
}
