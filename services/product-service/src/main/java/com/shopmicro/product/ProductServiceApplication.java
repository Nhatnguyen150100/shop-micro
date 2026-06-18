package com.shopmicro.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
// Nạp cả entity OutboxEvent của common-lib (cho Transactional Outbox)
@EntityScan("com.shopmicro")
@SpringBootApplication(scanBasePackages = {"com.shopmicro.product", "com.shopmicro.common"})
public class ProductServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(ProductServiceApplication.class, args);
  }
}
