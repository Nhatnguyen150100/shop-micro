package com.shopmicro.product.messaging;

import com.shopmicro.common.event.EventTopics;
import com.shopmicro.common.event.SagaCommand;
import com.shopmicro.common.event.SagaReply;
import com.shopmicro.common.outbox.OutboxPublisher;
import com.shopmicro.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Participant KHO HÀNG trong saga (orchestration): thực thi lệnh của orchestrator
 * rồi trả kết quả.
 *
 * <ul>
 *   <li>saga.cmd.reserve-stock -> trừ kho -> reply success/fail</li>
 *   <li>saga.cmd.release-stock -> hoàn kho (bù trừ) -> reply</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryListener {

  private final ProductService productService;
  private final OutboxPublisher outboxPublisher;

  // @Transactional: trừ kho + ghi reply vào outbox NẰM CÙNG MỘT transaction
  @KafkaListener(topics = EventTopics.CMD_RESERVE_STOCK, groupId = "product-service")
  @Transactional
  public void onReserveStock(SagaCommand cmd) {
    log.info("[product-service] Nhận reserve-stock cho đơn {}", cmd.orderId());
    boolean reserved = productService.reserveStock(cmd.productId(), cmd.quantity());
    reply(cmd, SagaCommand.RESERVE_STOCK, reserved,
        reserved ? "Đã trừ kho" : "Hết hàng");
  }

  @KafkaListener(topics = EventTopics.CMD_RELEASE_STOCK, groupId = "product-service")
  @Transactional
  public void onReleaseStock(SagaCommand cmd) {
    log.info("[product-service] Nhận release-stock (bù trừ) cho đơn {}", cmd.orderId());
    productService.releaseStock(cmd.productId(), cmd.quantity());
    reply(cmd, SagaCommand.RELEASE_STOCK, true, "Đã hoàn kho");
  }

  private void reply(SagaCommand cmd, String commandType, boolean success, String reason) {
    SagaReply reply = new SagaReply(cmd.sagaId(), cmd.orderId(), commandType, success, reason);
    outboxPublisher.publish(EventTopics.SAGA_REPLY, cmd.orderId().toString(), reply);
    log.info("[product-service] Reply {} (success={}) cho đơn {}", commandType, success, cmd.orderId());
  }
}
