package com.shopmicro.payment.messaging;

import com.shopmicro.common.event.EventTopics;
import com.shopmicro.common.event.SagaCommand;
import com.shopmicro.common.event.SagaReply;
import com.shopmicro.common.outbox.OutboxPublisher;
import com.shopmicro.payment.entity.Payment;
import com.shopmicro.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Participant THANH TOÁN trong saga (orchestration): chỉ thực thi lệnh do
 * orchestrator gửi rồi trả kết quả, KHÔNG biết bước trước/sau.
 *
 * <ul>
 *   <li>saga.cmd.process-payment -> trừ tiền -> reply success/fail</li>
 *   <li>saga.cmd.refund-payment  -> HOÀN TIỀN (bù trừ) -> reply</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentListener {

  private static final BigDecimal LIMIT = new BigDecimal("1000000000"); // 1 tỷ: quá hạn mức -> fail

  private final PaymentRepository paymentRepository;
  private final OutboxPublisher outboxPublisher;

  @KafkaListener(topics = EventTopics.CMD_PROCESS_PAYMENT, groupId = "payment-service")
  @Transactional
  public void onProcessPayment(SagaCommand cmd) {
    // Idempotency: nếu đã xử lý đơn này rồi thì không trừ tiền lần nữa, nhưng vẫn
    // reply lại (reply trước đó có thể đã mất) để orchestrator tiếp tục được.
    if (paymentRepository.findByOrderId(cmd.orderId()).isPresent()) {
      log.warn("Đơn {} đã có payment, không trừ lại (idempotent)", cmd.orderId());
      reply(cmd, SagaCommand.PROCESS_PAYMENT, true, "Đã thanh toán trước đó");
      return;
    }

    boolean ok = cmd.amount().compareTo(LIMIT) <= 0;
    String status = ok ? "COMPLETED" : "FAILED";

    paymentRepository.save(Payment.builder()
        .orderId(cmd.orderId())
        .userId(cmd.userId())
        .amount(cmd.amount())
        .status(status)
        .build());

    log.info("[payment] Đơn {} -> {}", cmd.orderId(), status);
    reply(cmd, SagaCommand.PROCESS_PAYMENT, ok, ok ? "Thanh toán thành công" : "Vượt hạn mức thanh toán");
  }

  @KafkaListener(topics = EventTopics.CMD_REFUND_PAYMENT, groupId = "payment-service")
  @Transactional
  public void onRefund(SagaCommand cmd) {
    paymentRepository.findByOrderId(cmd.orderId()).ifPresent(payment -> {
      payment.setStatus("REFUNDED");
      paymentRepository.save(payment);
      log.info("[payment] Đã HOÀN TIỀN cho đơn {}", cmd.orderId());
    });
    reply(cmd, SagaCommand.REFUND_PAYMENT, true, "Đã hoàn tiền");
  }

  private void reply(SagaCommand cmd, String commandType, boolean success, String reason) {
    SagaReply reply = new SagaReply(cmd.sagaId(), cmd.orderId(), commandType, success, reason);
    // Ghi reply vào outbox trong cùng transaction với việc lưu/cập nhật payment
    outboxPublisher.publish(EventTopics.SAGA_REPLY, cmd.orderId().toString(), reply);
  }
}
