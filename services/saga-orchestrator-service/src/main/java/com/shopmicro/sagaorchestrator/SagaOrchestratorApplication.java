package com.shopmicro.sagaorchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
// Nạp cả entity OutboxEvent của common-lib (cho Transactional Outbox)
@EntityScan("com.shopmicro")
@SpringBootApplication(scanBasePackages = {"com.shopmicro.sagaorchestrator", "com.shopmicro.common"})
public class SagaOrchestratorApplication {
  public static void main(String[] args) {
    SpringApplication.run(SagaOrchestratorApplication.class, args);
  }
}
