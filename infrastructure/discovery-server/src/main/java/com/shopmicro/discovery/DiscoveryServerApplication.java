package com.shopmicro.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Server: "danh bạ" của hệ thống.
 * Mỗi service khi khởi động sẽ tự đăng ký tên + địa chỉ vào đây,
 * và tra cứu địa chỉ của service khác qua TÊN (vd "PRODUCT-SERVICE") thay vì IP cứng.
 */
@EnableEurekaServer
@SpringBootApplication
public class DiscoveryServerApplication {
  public static void main(String[] args) {
    SpringApplication.run(DiscoveryServerApplication.class, args);
  }
}
