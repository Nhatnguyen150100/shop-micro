package com.shopmicro.common.event;

/**
 * Tên các Kafka topic dùng trong luồng Saga "Đặt hàng".
 * Đặt tập trung ở common-lib để mọi service tham chiếu cùng một hằng số,
 * tránh gõ sai tên topic (lỗi rất khó debug trong hệ phân tán).
 */
public final class EventTopics {

  private EventTopics() {
  }

  // order-service phát ra khi tạo đơn mới
  public static final String ORDER_CREATED = "order.created";

  // payment-service phát ra sau khi xử lý thanh toán
  public static final String PAYMENT_COMPLETED = "payment.completed";
  public static final String PAYMENT_FAILED = "payment.failed";

  // order-service phát ra để yêu cầu hoàn tiền (bước bù trừ - compensation)
  public static final String PAYMENT_REFUND = "payment.refund";

  // product-service phát ra sau khi trừ/không trừ được kho
  public static final String INVENTORY_RESERVED = "inventory.reserved";
  public static final String INVENTORY_FAILED = "inventory.failed";

  // order-service phát ra trạng thái cuối cùng để notification-service gửi mail
  public static final String ORDER_CONFIRMED = "order.confirmed";
  public static final String ORDER_CANCELLED = "order.cancelled";
}
