package com.shopmicro.common.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * LỆNH (command) mà saga-orchestrator-service gửi tới một participant
 * (payment/product/order) qua các topic saga.cmd.*.
 *
 * <p>Mang theo đủ dữ liệu để participant thực thi mà không phải gọi ngược lại
 * orchestrator. Trường {@code type} là một trong các hằng số bên dưới, dùng để
 * gắn nhãn lệnh và tương quan (correlate) với reply trả về.
 */
public record SagaCommand(
    UUID sagaId,
    UUID orderId,
    UUID userId,
    String email,
    UUID productId,
    int quantity,
    BigDecimal amount,
    String type,
    String reason) {

  // Các loại lệnh (tương ứng từng topic saga.cmd.*)
  public static final String PROCESS_PAYMENT = "PROCESS_PAYMENT";
  public static final String REFUND_PAYMENT = "REFUND_PAYMENT";
  public static final String RESERVE_STOCK = "RESERVE_STOCK";
  public static final String RELEASE_STOCK = "RELEASE_STOCK";
  public static final String CONFIRM_ORDER = "CONFIRM_ORDER";
  public static final String CANCEL_ORDER = "CANCEL_ORDER";
}
