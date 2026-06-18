package com.shopmicro.sagaorchestrator.entity;

import com.shopmicro.common.entity.BaseEntity;
import com.shopmicro.sagaorchestrator.enums.ESagaStep;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * "Nhật ký saga" (saga log): trạng thái của MỘT lần chạy saga đặt hàng.
 * Orchestrator dựa hoàn toàn vào bản ghi này để biết đang ở bước nào và
 * cần làm gì khi nhận reply — không cần gọi ngược các service.
 *
 * <p>{@code id} (kế thừa từ BaseEntity) chính là sagaId. Có một saga / một đơn.
 */
@Entity
@Table(name = "saga_state")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaState extends BaseEntity {

  @Column(nullable = false, unique = true)
  private UUID orderId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ESagaStep step;

  // payload cần để dựng các command tiếp theo mà không phải gọi ngược service
  @Column(nullable = false)
  private UUID userId;

  private String email;

  @Column(nullable = false)
  private UUID productId;

  @Column(nullable = false)
  private int quantity;

  @Column(nullable = false)
  private BigDecimal amount;

  private String lastError;

  // Optimistic lock: chặn 2 reply trùng cùng cập nhật state (idempotency cấp DB)
  @Version
  private Long version;
}
