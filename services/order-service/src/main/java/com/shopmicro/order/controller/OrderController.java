package com.shopmicro.order.controller;

import com.shopmicro.common.response.BaseResponse;
import com.shopmicro.common.response.ResponseBuilder;
import com.shopmicro.common.security.AuthHeaders;
import com.shopmicro.order.dto.CreateOrderRequest;
import com.shopmicro.order.entity.Order;
import com.shopmicro.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order", description = "Đặt hàng và theo dõi đơn (Saga)")
public class OrderController {

  private final OrderService orderService;

  @PostMapping
  @Operation(summary = "Tạo đơn hàng mới (khởi động Saga)")
  public ResponseEntity<BaseResponse<Order>> create(
      @RequestHeader(AuthHeaders.USER_ID) UUID userId,
      @RequestHeader(AuthHeaders.USER_EMAIL) String email,
      @Valid @RequestBody CreateOrderRequest req) {
    return ResponseBuilder.created("Đã tạo đơn, đang xử lý", orderService.createOrder(userId, email, req));
  }

  @GetMapping
  @Operation(summary = "Danh sách đơn của tôi")
  public ResponseEntity<BaseResponse<List<Order>>> myOrders(
      @RequestHeader(AuthHeaders.USER_ID) UUID userId) {
    return ResponseBuilder.success("OK", orderService.findByUser(userId));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Chi tiết đơn (xem trạng thái Saga: PENDING/CONFIRMED/CANCELLED)")
  public ResponseEntity<BaseResponse<Order>> get(@PathVariable UUID id) {
    return ResponseBuilder.success("OK", orderService.findById(id));
  }
}
