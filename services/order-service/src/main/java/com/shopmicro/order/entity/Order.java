package com.shopmicro.order.entity;

import com.shopmicro.common.entity.BaseEntity;
import com.shopmicro.order.enums.EOrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseEntity {

  @Column(nullable = false)
  private UUID userId;

  private String email;

  @Column(nullable = false)
  private UUID productId;

  @Column(nullable = false)
  private int quantity;

  @Column(nullable = false)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private EOrderStatus status = EOrderStatus.PENDING;

  private String note;
}
