package com.shopmicro.sagaorchestrator.messaging;

import com.shopmicro.common.event.EventTopics;
import com.shopmicro.common.event.OrderCreatedEvent;
import com.shopmicro.sagaorchestrator.orchestrator.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Điểm KHỞI ĐỘNG saga: lắng nghe order.created (do order-service phát khi tạo đơn)
 * và bàn giao cho orchestrator bắt đầu chuỗi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaTriggerListener {

  private final OrderSagaOrchestrator orchestrator;

  @KafkaListener(topics = EventTopics.ORDER_CREATED, groupId = "saga-orchestrator-service")
  public void onOrderCreated(OrderCreatedEvent event) {
    log.info("[orchestrator] Nhận order.created cho đơn {}", event.orderId());
    orchestrator.start(event);
  }
}
