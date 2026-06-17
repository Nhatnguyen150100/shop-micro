package com.shopmicro.product.config;

import com.shopmicro.product.entity.Product;
import com.shopmicro.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Nạp sẵn vài sản phẩm mẫu khi DB trống — để có dữ liệu thử luồng đặt hàng ngay.
 * (Gọi GET /api/products để lấy ID sản phẩm dùng cho việc tạo đơn.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

  private final ProductRepository repository;

  @Override
  public void run(String... args) {
    if (repository.count() > 0) {
      return;
    }
    repository.saveAll(List.of(
        Product.builder().name("Laptop Dell XPS 13").description("Ultrabook 13 inch")
            .price(new BigDecimal("32000000")).stock(10).build(),
        Product.builder().name("iPhone 15 Pro").description("256GB Titan")
            .price(new BigDecimal("28000000")).stock(5).build(),
        Product.builder().name("Tai nghe Sony WH-1000XM5").description("Chống ồn")
            .price(new BigDecimal("8000000")).stock(0).build() // hết hàng để test luồng FAILED
    ));
    log.info("Đã nạp sản phẩm mẫu.");
  }
}
