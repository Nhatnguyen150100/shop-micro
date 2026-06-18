package com.shopmicro.sagaorchestrator.repository;

import com.shopmicro.sagaorchestrator.entity.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SagaStateRepository extends JpaRepository<SagaState, UUID> {
  Optional<SagaState> findByOrderId(UUID orderId);
}
