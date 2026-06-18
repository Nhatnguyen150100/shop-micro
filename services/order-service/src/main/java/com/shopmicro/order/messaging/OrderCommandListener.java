package com.shopmicro.order.messaging;

import com.shopmicro.common.event.EventTopics;
import com.shopmicro.common.event.OrderStatusEvent;
import com.shopmicro.common.event.SagaCommand;
import com.shopmicro.common.outbox.OutboxPublisher;
import com.shopmicro.order.entity.Order;
import com.shopmicro.order.enums.EOrderStatus;
import com.shopmicro.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * order-service trong mô hình ORCHESTRATION chỉ là "tay chân": nhận lệnh từ
 * orchestrator để chốt trạng thái đơn, rồi phát order.confirmed/order.cancelled
 * cho notification-service. order-service KHÔNG còn tự điều phối saga nữa
 * (toàn bộ logic đó đã chuyển sang saga-orchestrator-service).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCommandListener {

  private final OrderRepository orderRepository;
  private final OutboxPublisher outboxPublisher;

  @KafkaListener(topics = EventTopics.CMD_CONFIRM_ORDER, groupId = "order-service")
  @Transactional
  public void onConfirmOrder(SagaCommand cmd) {
    apply(cmd, EOrderStatus.CONFIRMED);
  }

  @KafkaListener(topics = EventTopics.CMD_CANCEL_ORDER, groupId = "order-service")
  @Transactional
  public void onCancelOrder(SagaCommand cmd) {
    apply(cmd, EOrderStatus.CANCELLED);
  }

  private void apply(SagaCommand cmd, EOrderStatus target) {
    Order order = orderRepository.findById(cmd.orderId()).orElse(null);
    // Idempotency: chỉ chuyển trạng thái khi đơn còn PENDING
    if (order == null || order.getStatus() != EOrderStatus.PENDING) {
      log.warn("Bỏ qua lệnh {} cho đơn {} (không tồn tại hoặc đã chốt)",
          cmd.type(), cmd.orderId());
      return;
    }
    order.setStatus(target);
    order.setNote(cmd.reason());
    orderRepository.save(order);
    log.info("Đơn {} -> {} ({})", order.getId(), target, cmd.reason());

    OrderStatusEvent evt = new OrderStatusEvent(
        order.getId(), order.getUserId(), order.getEmail(), target.name(), cmd.reason());
    String topic = target == EOrderStatus.CONFIRMED
        ? EventTopics.ORDER_CONFIRMED : EventTopics.ORDER_CANCELLED;
    // Ghi vào outbox trong cùng transaction với việc đổi trạng thái đơn
    outboxPublisher.publish(topic, order.getId().toString(), evt);
  }
}
