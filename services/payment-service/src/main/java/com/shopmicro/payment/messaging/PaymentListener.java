package com.shopmicro.payment.messaging;

import com.shopmicro.common.event.EventTopics;
import com.shopmicro.common.event.OrderCreatedEvent;
import com.shopmicro.common.event.PaymentEvent;
import com.shopmicro.payment.entity.Payment;
import com.shopmicro.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * BƯỚC 1 của Saga: lắng nghe "order.created" -> xử lý thanh toán
 * -> phát "payment.completed" hoặc "payment.failed".
 * Đồng thời lắng nghe "payment.refund" để HOÀN TIỀN (bù trừ).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentListener {

  private static final BigDecimal LIMIT = new BigDecimal("1000000000"); // 1 tỷ: quá hạn mức -> fail

  private final PaymentRepository paymentRepository;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  @KafkaListener(topics = EventTopics.ORDER_CREATED, groupId = "payment-service")
  @Transactional
  public void onOrderCreated(OrderCreatedEvent event) {
    // Idempotency: nếu đã xử lý đơn này rồi thì bỏ qua (tránh trừ tiền 2 lần khi message lặp)
    if (paymentRepository.findByOrderId(event.orderId()).isPresent()) {
      log.warn("Đơn {} đã có payment, bỏ qua (idempotent)", event.orderId());
      return;
    }

    boolean ok = event.amount().compareTo(LIMIT) <= 0;
    String status = ok ? "COMPLETED" : "FAILED";

    paymentRepository.save(Payment.builder()
        .orderId(event.orderId())
        .userId(event.userId())
        .amount(event.amount())
        .status(status)
        .build());

    PaymentEvent result = new PaymentEvent(
        event.orderId(), event.userId(), event.email(),
        event.productId(), event.quantity(), event.amount(), status);

    String topic = ok ? EventTopics.PAYMENT_COMPLETED : EventTopics.PAYMENT_FAILED;
    kafkaTemplate.send(topic, event.orderId().toString(), result);
    log.info("[payment] Đơn {} -> {} (phát {})", event.orderId(), status, topic);
  }

  @KafkaListener(topics = EventTopics.PAYMENT_REFUND, groupId = "payment-service")
  @Transactional
  public void onRefund(PaymentEvent event) {
    paymentRepository.findByOrderId(event.orderId()).ifPresent(payment -> {
      payment.setStatus("REFUNDED");
      paymentRepository.save(payment);
      log.info("[payment] Đã HOÀN TIỀN cho đơn {}", event.orderId());
    });
  }
}
