package com.shopmicro.order.service;

import com.shopmicro.common.event.EventTopics;
import com.shopmicro.common.event.OrderCreatedEvent;
import com.shopmicro.common.exception.BadRequestException;
import com.shopmicro.common.exception.ResourceNotFoundException;
import com.shopmicro.order.client.ProductClient;
import com.shopmicro.order.dto.CreateOrderRequest;
import com.shopmicro.order.dto.ProductDto;
import com.shopmicro.order.entity.Order;
import com.shopmicro.order.enums.EOrderStatus;
import com.shopmicro.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;
  private final ProductClient productClient;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  /**
   * BƯỚC 0 của Saga:
   * 1) Gọi ĐỒNG BỘ product-service (Feign) để lấy giá + kiểm tra tồn tại.
   * 2) Lưu đơn trạng thái PENDING.
   * 3) Phát event "order.created" để khởi động chuỗi Saga (thanh toán → trừ kho).
   */
  @Transactional
  public Order createOrder(UUID userId, String email, CreateOrderRequest req) {
    ProductDto product = productClient.getById(req.productId()).data();
    if (product == null) {
      throw new ResourceNotFoundException("Sản phẩm không tồn tại");
    }
    if (product.stock() < req.quantity()) {
      throw new BadRequestException("Sản phẩm không đủ tồn kho");
    }

    BigDecimal amount = product.price().multiply(BigDecimal.valueOf(req.quantity()));

    Order order = orderRepository.save(Order.builder()
        .userId(userId)
        .email(email)
        .productId(req.productId())
        .quantity(req.quantity())
        .amount(amount)
        .status(EOrderStatus.PENDING)
        .build());

    OrderCreatedEvent event = new OrderCreatedEvent(
        order.getId(), userId, email, req.productId(), req.quantity(), amount);
    kafkaTemplate.send(EventTopics.ORDER_CREATED, order.getId().toString(), event);

    log.info("Tạo đơn {} (PENDING) và phát order.created", order.getId());
    return order;
  }

  public List<Order> findByUser(UUID userId) {
    return orderRepository.findByUserId(userId);
  }

  public Order findById(UUID id) {
    return orderRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn: " + id));
  }
}
