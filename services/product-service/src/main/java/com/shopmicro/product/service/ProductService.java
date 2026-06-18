package com.shopmicro.product.service;

import com.shopmicro.common.exception.ResourceNotFoundException;
import com.shopmicro.product.dto.CreateProductRequest;
import com.shopmicro.product.entity.Product;
import com.shopmicro.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository repository;

  public List<Product> findAll() {
    return repository.findAll();
  }

  public Product findById(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + id));
  }

  @Transactional
  public Product create(CreateProductRequest req) {
    Product product = Product.builder()
        .name(req.name())
        .description(req.description())
        .price(req.price())
        .stock(req.stock())
        .build();
    return repository.save(product);
  }

  /**
   * Trừ kho cho đơn hàng (một bước trong Saga). Trả về true nếu đủ hàng.
   * Dùng @Transactional để đảm bảo đọc-ghi tồn kho an toàn.
   */
  @Transactional
  public boolean reserveStock(UUID productId, int quantity) {
    Product product = repository.findById(productId).orElse(null);
    if (product == null || product.getStock() < quantity) {
      log.warn("Trừ kho THẤT BẠI cho sản phẩm {} (cần {}, còn {})",
          productId, quantity, product == null ? 0 : product.getStock());
      return false;
    }
    product.setStock(product.getStock() - quantity);
    repository.save(product);
    log.info("Trừ kho THÀNH CÔNG: sản phẩm {} -{} (còn {})",
        productId, quantity, product.getStock());
    return true;
  }

  /**
   * Hoàn kho (hành động BÙ TRỪ cho reserveStock khi saga bị huỷ ở bước sau).
   */
  @Transactional
  public void releaseStock(UUID productId, int quantity) {
    repository.findById(productId).ifPresent(product -> {
      product.setStock(product.getStock() + quantity);
      repository.save(product);
      log.info("Hoàn kho: sản phẩm {} +{} (còn {})", productId, quantity, product.getStock());
    });
  }
}
