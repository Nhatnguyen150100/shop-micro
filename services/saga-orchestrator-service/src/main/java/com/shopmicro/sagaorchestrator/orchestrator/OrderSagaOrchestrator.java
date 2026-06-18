package com.shopmicro.sagaorchestrator.orchestrator;

import com.shopmicro.common.event.EventTopics;
import com.shopmicro.common.event.OrderCreatedEvent;
import com.shopmicro.common.event.SagaCommand;
import com.shopmicro.common.event.SagaReply;
import com.shopmicro.common.outbox.OutboxPublisher;
import com.shopmicro.sagaorchestrator.entity.SagaState;
import com.shopmicro.sagaorchestrator.enums.ESagaStep;
import com.shopmicro.sagaorchestrator.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * NHẠC TRƯỞNG (orchestrator) của Saga đặt hàng.
 *
 * <p>Toàn bộ logic điều phối nằm tập trung tại đây: orchestrator phát COMMAND tới
 * từng participant rồi nhận REPLY để quyết định bước kế tiếp hoặc BÙ TRỪ
 * (compensation). Trạng thái mỗi saga được lưu trong bảng {@code saga_state} nên
 * có thể quan sát/khôi phục — khác hẳn choreography (logic rải rác, không có nơi
 * nào nắm toàn cảnh).
 *
 * <pre>
 *  order.created  ─► start()      : tạo saga, gửi process-payment
 *  reply PROCESS_PAYMENT  OK      : ─► reserve-stock
 *                         FAIL    : ─► cancel-order (chưa trừ gì, không cần bù trừ)
 *  reply RESERVE_STOCK    OK      : ─► confirm-order (THÀNH CÔNG)
 *                         FAIL    : ─► refund-payment (BÙ TRỪ)
 *  reply REFUND_PAYMENT   OK      : ─► cancel-order (đã hoàn tiền)
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

  private final SagaStateRepository sagaRepository;
  private final OutboxPublisher outboxPublisher;

  /** Khởi động saga khi order-service phát order.created. */
  @Transactional
  public void start(OrderCreatedEvent event) {
    // Idempotency: nếu đã có saga cho đơn này thì bỏ qua (order.created lặp)
    if (sagaRepository.findByOrderId(event.orderId()).isPresent()) {
      log.warn("Saga cho đơn {} đã tồn tại, bỏ qua (idempotent)", event.orderId());
      return;
    }

    SagaState saga = sagaRepository.save(SagaState.builder()
        .orderId(event.orderId())
        .userId(event.userId())
        .email(event.email())
        .productId(event.productId())
        .quantity(event.quantity())
        .amount(event.amount())
        .step(ESagaStep.STARTED)
        .build());

    log.info("Khởi động saga {} cho đơn {}", saga.getId(), saga.getOrderId());

    saga.setStep(ESagaStep.PAYMENT_PENDING);
    send(saga, EventTopics.CMD_PROCESS_PAYMENT, SagaCommand.PROCESS_PAYMENT, null);
  }

  /** Xử lý reply từ participant và đẩy saga sang bước kế tiếp. */
  @Transactional
  public void onReply(SagaReply reply) {
    SagaState saga = sagaRepository.findById(reply.sagaId()).orElse(null);
    if (saga == null) {
      log.warn("Không tìm thấy saga {} cho reply {}", reply.sagaId(), reply.commandType());
      return;
    }

    switch (reply.commandType()) {
      case SagaCommand.PROCESS_PAYMENT -> onPaymentReply(saga, reply);
      case SagaCommand.RESERVE_STOCK -> onStockReply(saga, reply);
      case SagaCommand.REFUND_PAYMENT -> onRefundReply(saga, reply);
      default -> log.warn("Bỏ qua reply không xác định: {}", reply.commandType());
    }
  }

  private void onPaymentReply(SagaState saga, SagaReply reply) {
    if (!expect(saga, ESagaStep.PAYMENT_PENDING, reply)) {
      return;
    }
    if (reply.success()) {
      saga.setStep(ESagaStep.STOCK_PENDING);
      send(saga, EventTopics.CMD_RESERVE_STOCK, SagaCommand.RESERVE_STOCK, null);
      log.info("Saga {}: thanh toán OK -> trừ kho", saga.getId());
    } else {
      // Chưa trừ kho, thanh toán cũng thất bại -> không cần bù trừ, chỉ huỷ đơn
      saga.setStep(ESagaStep.FAILED);
      saga.setLastError(reply.reason());
      send(saga, EventTopics.CMD_CANCEL_ORDER, SagaCommand.CANCEL_ORDER, "Thanh toán thất bại");
      log.info("Saga {}: thanh toán FAIL -> huỷ đơn (không bù trừ)", saga.getId());
    }
  }

  private void onStockReply(SagaState saga, SagaReply reply) {
    if (!expect(saga, ESagaStep.STOCK_PENDING, reply)) {
      return;
    }
    if (reply.success()) {
      saga.setStep(ESagaStep.COMPLETED);
      send(saga, EventTopics.CMD_CONFIRM_ORDER, SagaCommand.CONFIRM_ORDER, "Đặt hàng thành công");
      log.info("Saga {}: trừ kho OK -> xác nhận đơn (HOÀN TẤT)", saga.getId());
    } else {
      // Tiền đã trừ ở bước trước -> phải BÙ TRỪ bằng lệnh hoàn tiền
      saga.setStep(ESagaStep.COMPENSATING);
      saga.setLastError(reply.reason());
      send(saga, EventTopics.CMD_REFUND_PAYMENT, SagaCommand.REFUND_PAYMENT, null);
      log.info("Saga {}: trừ kho FAIL -> hoàn tiền (BÙ TRỪ)", saga.getId());
    }
  }

  private void onRefundReply(SagaState saga, SagaReply reply) {
    if (!expect(saga, ESagaStep.COMPENSATING, reply)) {
      return;
    }
    if (reply.success()) {
      saga.setStep(ESagaStep.CANCELLED);
      send(saga, EventTopics.CMD_CANCEL_ORDER, SagaCommand.CANCEL_ORDER, "Hết hàng - đã hoàn tiền");
      log.info("Saga {}: hoàn tiền xong -> huỷ đơn (BÙ TRỪ HOÀN TẤT)", saga.getId());
    } else {
      // Hoàn tiền lỗi: giữ nguyên COMPENSATING để retry/can thiệp thủ công
      saga.setLastError("Hoàn tiền thất bại: " + reply.reason());
      log.error("Saga {}: HOÀN TIỀN THẤT BẠI -> cần can thiệp thủ công", saga.getId());
    }
  }

  /** Guard idempotency: chỉ xử lý reply nếu saga đang đúng bước kỳ vọng. */
  private boolean expect(SagaState saga, ESagaStep expected, SagaReply reply) {
    if (saga.getStep() != expected) {
      log.warn("Saga {}: bỏ qua reply {} (đang ở {}, kỳ vọng {})",
          saga.getId(), reply.commandType(), saga.getStep(), expected);
      return false;
    }
    return true;
  }

  /** Gửi một command tới participant. Topic xác định ai nhận, type để correlate reply. */
  private void send(SagaState saga, String topic, String type, String reason) {
    UUID sagaId = saga.getId();
    SagaCommand cmd = new SagaCommand(
        sagaId, saga.getOrderId(), saga.getUserId(), saga.getEmail(),
        saga.getProductId(), saga.getQuantity(), saga.getAmount(), type, reason);
    // Ghi command vào outbox trong cùng transaction với cập nhật saga_state
    outboxPublisher.publish(topic, saga.getOrderId().toString(), cmd);
    log.info("Saga {} -> ghi {} vào outbox ({})", sagaId, type, topic);
  }
}
