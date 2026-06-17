package com.shopmicro.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Config Server: phục vụ cấu hình tập trung cho mọi service.
 * Ở đây dùng chế độ "native" (đọc file YAML trong thư mục config),
 * thực tế production thường trỏ vào một Git repository.
 */
@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication {
  public static void main(String[] args) {
    SpringApplication.run(ConfigServerApplication.class, args);
  }
}
