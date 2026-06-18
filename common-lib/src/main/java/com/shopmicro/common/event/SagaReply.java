package com.shopmicro.common.event;

import java.util.UUID;

/**
 * KẾT QUẢ (reply) mà participant trả về cho saga-orchestrator-service sau khi
 * thực thi một {@link SagaCommand}. Mọi reply đều về chung topic saga.reply.
 *
 * @param sagaId      định danh saga (để orchestrator tra cứu state)
 * @param orderId     đơn liên quan
 * @param commandType loại lệnh đã xử lý (xem hằng số trong {@link SagaCommand})
 * @param success     true nếu bước thành công, false nếu thất bại
 * @param reason      mô tả lý do (đặc biệt khi thất bại)
 */
public record SagaReply(
    UUID sagaId,
    UUID orderId,
    String commandType,
    boolean success,
    String reason) {
}
