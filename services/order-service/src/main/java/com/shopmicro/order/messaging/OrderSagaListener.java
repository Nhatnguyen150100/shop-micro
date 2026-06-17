package com.shopmicro.order.messaging;

import com.shopmicro.common.event.EventTopics;
import com.shopmicro.common.event.InventoryEvent;
import com.shopmicro.common.event.OrderStatusEvent;
import com.shopmicro.common.event.PaymentEvent;
import com.shopmicro.order.entity.Order;
import com.shopmicro.order.enums.EOrderStatus;
import com.shopmicro.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * BỘ ĐIỀU PHỐI SAGA (phía order-service): lắng nghe kết quả các bước và quyết định
 * trạng thái cuối của đơn hàng, đồng thời kích hoạt BÙ TRỪ (compensation) khi cần.
 *
 * <pre>
 *  payment.failed      -> CANCELLED (chưa trừ kho nên không cần bù trừ)
 *  inventory.reserved  -> CONFIRMED -> phát order.confirmed
 *  inventory.failed    -> CANCELLED -> phát payment.refund (HOÀN TIỀN) + order.cancelled
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaListener {

  private final OrderRepository orderRepository;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  @KafkaListener(topics = EventTopics.PAYMENT_FAILED, groupId = "order-service")
  @Transactional
  public void onPaymentFailed(PaymentEvent event) {
    Order order = cancel(event.orderId(), "Thanh toán thất bại");
    if (order != null) {
      publishStatus(order, "CANCELLED", "Thanh toán thất bại");
    }
  }

  @KafkaListener(topics = EventTopics.INVENTORY_RESERVED, groupId = "order-service")
  @Transactional
  public void onInventoryReserved(InventoryEvent event) {
    Order order = orderRepository.findById(event.orderId()).orElse(null);
    if (order == null || order.getStatus() != EOrderStatus.PENDING) {
      return;
    }
    order.setStatus(EOrderStatus.CONFIRMED);
    order.setNote("Đặt hàng thành công");
    orderRepository.save(order);
    log.info("Đơn {} -> CONFIRMED", order.getId());
    publishStatus(order, "CONFIRMED", "Đặt hàng thành công");
  }

  @KafkaListener(topics = EventTopics.INVENTORY_FAILED, groupId = "order-service")
  @Transactional
  public void onInventoryFailed(InventoryEvent event) {
    Order order = cancel(event.orderId(), "Hết hàng");
    if (order == null) {
      return;
    }
    // BÙ TRỪ: tiền đã trừ ở bước thanh toán -> yêu cầu payment-service hoàn lại
    PaymentEvent refund = new PaymentEvent(
        event.orderId(), event.userId(), event.email(),
        event.productId(), event.quantity(), event.amount(), "REFUND");
    kafkaTemplate.send(EventTopics.PAYMENT_REFUND, event.orderId().toString(), refund);
    log.info("Đơn {} -> CANCELLED, phát payment.refund (bù trừ)", order.getId());

    publishStatus(order, "CANCELLED", "Hết hàng - đã hoàn tiền");
  }

  private Order cancel(UUID orderId, String reason) {
    Order order = orderRepository.findById(orderId).orElse(null);
    if (order == null || order.getStatus() != EOrderStatus.PENDING) {
      return null;
    }
    order.setStatus(EOrderStatus.CANCELLED);
    order.setNote(reason);
    return orderRepository.save(order);
  }

  private void publishStatus(Order order, String status, String reason) {
    OrderStatusEvent evt = new OrderStatusEvent(
        order.getId(), order.getUserId(), order.getEmail(), status, reason);
    String topic = "CONFIRMED".equals(status) ? EventTopics.ORDER_CONFIRMED : EventTopics.ORDER_CANCELLED;
    kafkaTemplate.send(topic, order.getId().toString(), evt);
  }
}
