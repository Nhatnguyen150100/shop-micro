package com.shopmicro.gateway.filter;

import com.shopmicro.common.security.AuthHeaders;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Bộ lọc XÁC THỰC TẬP TRUNG cho toàn hệ thống.
 *
 * <p>Đây là điểm mấu chốt của mô hình "Gateway-centric security":
 * <ol>
 *   <li>Với route công khai (đăng nhập, swagger, xem sản phẩm) → cho qua.</li>
 *   <li>Với route cần bảo vệ → kiểm tra & verify chữ ký JWT NGAY TẠI ĐÂY.</li>
 *   <li>Nếu hợp lệ → bóc thông tin user, gắn vào header X-User-* rồi chuyển xuống
 *       service nội bộ. Service nội bộ KHÔNG cần tự verify JWT nữa.</li>
 *   <li>Nếu sai → trả 401 ngay, request không bao giờ chạm tới service.</li>
 * </ol>
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

  @Value("${jwt.secret}")
  private String secret;

  /** Các tiền tố đường dẫn KHÔNG cần đăng nhập. */
  private static final List<String> PUBLIC_PREFIXES = List.of(
      "/api/auth",
      "/v3/api-docs",
      "/swagger-ui",
      "/actuator");

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String path = request.getURI().getPath();

    if (isPublic(path, request.getMethod())) {
      return chain.filter(exchange);
    }

    String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return unauthorized(exchange, "Thiếu Authorization header");
    }

    String token = authHeader.substring(7);
    try {
      Claims claims = Jwts.parser()
          .verifyWith(signingKey())
          .build()
          .parseSignedClaims(token)
          .getPayload();

      // Gắn thông tin user đã xác thực vào header để truyền xuống service nội bộ
      ServerHttpRequest mutated = request.mutate()
          .header(AuthHeaders.USER_ID, String.valueOf(claims.get("id")))
          .header(AuthHeaders.USER_EMAIL, claims.getSubject())
          .header(AuthHeaders.USER_ROLE, String.valueOf(claims.get("role")))
          .build();

      return chain.filter(exchange.mutate().request(mutated).build());
    } catch (Exception e) {
      log.warn("JWT không hợp lệ: {}", e.getMessage());
      return unauthorized(exchange, "Token không hợp lệ hoặc đã hết hạn");
    }
  }

  private boolean isPublic(String path, HttpMethod method) {
    if (PUBLIC_PREFIXES.stream().anyMatch(path::startsWith)) {
      return true;
    }
    // Cho phép xem danh sách/chi tiết sản phẩm mà không cần đăng nhập
    return HttpMethod.GET.equals(method) && path.startsWith("/api/products");
  }

  private SecretKey signingKey() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    exchange.getResponse().getHeaders().add("Content-Type", "application/json; charset=UTF-8");
    String body = "{\"success\":false,\"status\":401,\"message\":\"" + message + "\"}";
    var buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes());
    return exchange.getResponse().writeWith(Mono.just(buffer));
  }

  @Override
  public int getOrder() {
    // Chạy sớm, trước các filter định tuyến
    return -1;
  }
}
