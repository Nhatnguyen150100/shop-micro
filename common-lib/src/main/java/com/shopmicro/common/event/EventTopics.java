package com.shopmicro.common.event;

/**
 * Tên các Kafka topic dùng trong luồng Saga "Đặt hàng" (kiểu ORCHESTRATION).
 * Đặt tập trung ở common-lib để mọi service tham chiếu cùng một hằng số,
 * tránh gõ sai tên topic (lỗi rất khó debug trong hệ phân tán).
 *
 * <p>Mô hình: saga-orchestrator-service là "nhạc trưởng". Nó phát các COMMAND
 * (saga.cmd.*) tới từng participant; participant thực thi rồi trả REPLY về một
 * topic duy nhất (saga.reply). Orchestrator dựa vào reply để quyết định bước kế
 * tiếp hoặc kích hoạt bù trừ (compensation).
 */
public final class EventTopics {

  private EventTopics() {
  }

  // order-service phát ra khi tạo đơn mới -> orchestrator dùng làm điểm KHỞI ĐỘNG saga
  public static final String ORDER_CREATED = "order.created";

  // ---- COMMAND: orchestrator -> participant ----
  public static final String CMD_PROCESS_PAYMENT = "saga.cmd.process-payment"; // -> payment-service
  public static final String CMD_REFUND_PAYMENT = "saga.cmd.refund-payment";   // -> payment-service (bù trừ)
  public static final String CMD_RESERVE_STOCK = "saga.cmd.reserve-stock";     // -> product-service
  public static final String CMD_RELEASE_STOCK = "saga.cmd.release-stock";     // -> product-service (bù trừ)
  public static final String CMD_CONFIRM_ORDER = "saga.cmd.confirm-order";     // -> order-service
  public static final String CMD_CANCEL_ORDER = "saga.cmd.cancel-order";       // -> order-service

  // ---- REPLY: participant -> orchestrator (gom mọi reply về một topic) ----
  public static final String SAGA_REPLY = "saga.reply";

  // order-service phát ra trạng thái cuối cùng để notification-service gửi mail
  public static final String ORDER_CONFIRMED = "order.confirmed";
  public static final String ORDER_CANCELLED = "order.cancelled";
}
