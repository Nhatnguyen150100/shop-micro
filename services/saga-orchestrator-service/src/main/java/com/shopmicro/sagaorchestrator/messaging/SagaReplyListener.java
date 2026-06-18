package com.shopmicro.sagaorchestrator.messaging;

import com.shopmicro.common.event.EventTopics;
import com.shopmicro.common.event.SagaReply;
import com.shopmicro.sagaorchestrator.orchestrator.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Lắng nghe MỌI reply từ các participant (payment/product) trên một topic duy nhất
 * (saga.reply) và bàn giao cho orchestrator quyết định bước kế tiếp / bù trừ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaReplyListener {

  private final OrderSagaOrchestrator orchestrator;

  @KafkaListener(topics = EventTopics.SAGA_REPLY, groupId = "saga-orchestrator-service")
  public void onReply(SagaReply reply) {
    log.info("[orchestrator] Nhận reply {} (success={}) cho saga {}",
        reply.commandType(), reply.success(), reply.sagaId());
    orchestrator.onReply(reply);
  }
}
