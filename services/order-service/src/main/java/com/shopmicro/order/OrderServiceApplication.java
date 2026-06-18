package com.shopmicro.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@EnableFeignClients
// Nạp cả entity OutboxEvent của common-lib (cho Transactional Outbox)
@EntityScan("com.shopmicro")
@SpringBootApplication(scanBasePackages = {"com.shopmicro.order", "com.shopmicro.common"})
public class OrderServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(OrderServiceApplication.class, args);
  }
}
