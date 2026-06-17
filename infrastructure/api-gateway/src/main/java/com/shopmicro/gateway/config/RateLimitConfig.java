package com.shopmicro.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Cấu hình rate limit tại Gateway.
 * KeyResolver quyết định "đếm theo cái gì": ở đây đếm theo địa chỉ IP của client.
 * (Thay thế cho RateLimitFilter trong boilerplate monolith gốc.)
 */
@Configuration
public class RateLimitConfig {

  @Bean
  public KeyResolver ipKeyResolver() {
    return exchange -> {
      String ip = exchange.getRequest().getRemoteAddress() != null
          ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
          : "unknown";
      return Mono.just(ip);
    };
  }
}
