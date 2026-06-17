package com.shopmicro.payment.entity;

import com.shopmicro.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {

  @Column(nullable = false)
  private UUID orderId;

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false)
  private String status; // COMPLETED, FAILED, REFUNDED
}
