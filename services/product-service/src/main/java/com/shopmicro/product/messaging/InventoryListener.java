package com.shopmicro.product.messaging;

import com.shopmicro.common.event.EventTopics;
import com.shopmicro.common.event.InventoryEvent;
import com.shopmicro.common.event.PaymentEvent;
import com.shopmicro.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * BƯỚC 2 của Saga (sau khi thanh toán thành công):
 * lắng nghe "payment.completed" → trừ kho → phát "inventory.reserved" hoặc "inventory.failed".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryListener {

  private final ProductService productService;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  @KafkaListener(topics = EventTopics.PAYMENT_COMPLETED, groupId = "product-service")
  public void onPaymentCompleted(PaymentEvent event) {
    log.info("[product-service] Nhận payment.completed cho đơn {}", event.orderId());

    boolean reserved = productService.reserveStock(event.productId(), event.quantity());

    InventoryEvent result = new InventoryEvent(
        event.orderId(), event.userId(), event.email(),
        event.productId(), event.quantity(), event.amount(),
        reserved ? "RESERVED" : "FAILED");

    String topic = reserved ? EventTopics.INVENTORY_RESERVED : EventTopics.INVENTORY_FAILED;
    kafkaTemplate.send(topic, event.orderId().toString(), result);
    log.info("[product-service] Đã phát {} cho đơn {}", topic, event.orderId());
  }
}
