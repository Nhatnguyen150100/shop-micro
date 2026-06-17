package com.shopmicro.user.controller;

import com.shopmicro.common.response.BaseResponse;
import com.shopmicro.common.response.ResponseBuilder;
import com.shopmicro.common.security.AuthHeaders;
import com.shopmicro.user.dto.UpdateProfileRequest;
import com.shopmicro.user.entity.UserProfile;
import com.shopmicro.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Lưu ý cách lấy danh tính: KHÔNG đọc JWT, mà đọc header X-User-* do Gateway gắn vào.
 * Đây là minh hoạ rõ nhất cho mô hình "Gateway xác thực tập trung".
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "Quản lý hồ sơ người dùng")
public class UserController {

  private final UserProfileService service;

  @GetMapping("/me")
  @Operation(summary = "Xem hồ sơ của chính mình")
  public ResponseEntity<BaseResponse<UserProfile>> me(
      @RequestHeader(AuthHeaders.USER_ID) UUID userId,
      @RequestHeader(AuthHeaders.USER_EMAIL) String email) {
    return ResponseBuilder.success("OK", service.getOrCreate(userId, email));
  }

  @PutMapping("/me")
  @Operation(summary = "Cập nhật hồ sơ của chính mình")
  public ResponseEntity<BaseResponse<UserProfile>> update(
      @RequestHeader(AuthHeaders.USER_ID) UUID userId,
      @RequestHeader(AuthHeaders.USER_EMAIL) String email,
      @RequestBody UpdateProfileRequest req) {
    return ResponseBuilder.success("Cập nhật thành công", service.update(userId, email, req));
  }
}
