package com.shopmicro.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * scanBasePackages bao gồm cả "com.shopmicro.common" để nạp được
 * GlobalExceptionHandler dùng chung từ common-lib.
 */
@EnableJpaAuditing
@SpringBootApplication(scanBasePackages = {"com.shopmicro.auth", "com.shopmicro.common"})
public class AuthServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(AuthServiceApplication.class, args);
  }
}
