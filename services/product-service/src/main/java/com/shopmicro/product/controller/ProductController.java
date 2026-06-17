package com.shopmicro.product.controller;

import com.shopmicro.common.response.BaseResponse;
import com.shopmicro.common.response.ResponseBuilder;
import com.shopmicro.product.dto.CreateProductRequest;
import com.shopmicro.product.entity.Product;
import com.shopmicro.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "Catalog sản phẩm và tồn kho")
public class ProductController {

  private final ProductService service;

  @GetMapping
  @Operation(summary = "Danh sách sản phẩm (công khai)")
  public ResponseEntity<BaseResponse<List<Product>>> list() {
    return ResponseBuilder.success("OK", service.findAll());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Chi tiết sản phẩm (công khai)")
  public ResponseEntity<BaseResponse<Product>> get(@PathVariable UUID id) {
    return ResponseBuilder.success("OK", service.findById(id));
  }

  @PostMapping
  @Operation(summary = "Tạo sản phẩm (cần đăng nhập)")
  public ResponseEntity<BaseResponse<Product>> create(@Valid @RequestBody CreateProductRequest req) {
    return ResponseBuilder.created("Tạo sản phẩm thành công", service.create(req));
  }
}
