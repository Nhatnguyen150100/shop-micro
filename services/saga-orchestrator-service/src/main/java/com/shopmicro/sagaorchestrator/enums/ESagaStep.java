package com.shopmicro.sagaorchestrator.enums;

/**
 * Các bước (state) của Saga đặt hàng theo mô hình orchestration.
 *
 * <pre>
 *  STARTED
 *     │ gửi process-payment
 *     ▼
 *  PAYMENT_PENDING ──(payment OK)──► STOCK_PENDING ──(stock OK)──► COMPLETED
 *     │ (payment FAIL)                    │ (stock FAIL)
 *     ▼                                   ▼
 *  FAILED  ─► cancel-order            COMPENSATING ─► refund-payment
 *                                         │ (refund OK)
 *                                         ▼
 *                                     CANCELLED ─► cancel-order
 * </pre>
 */
public enum ESagaStep {
  STARTED,          // saga vừa khởi tạo
  PAYMENT_PENDING,  // đã gửi lệnh trừ tiền, chờ reply
  STOCK_PENDING,    // đã gửi lệnh trừ kho, chờ reply
  COMPENSATING,     // đang bù trừ (đã gửi lệnh hoàn tiền, chờ reply)
  COMPLETED,        // thành công - đơn được xác nhận
  CANCELLED,        // thất bại sau khi bù trừ xong
  FAILED            // thất bại sớm (chưa cần bù trừ)
}
