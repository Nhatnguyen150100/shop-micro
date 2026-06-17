package com.shopmicro.auth.service;

import com.shopmicro.auth.dto.AuthResponse;
import com.shopmicro.auth.dto.LoginRequest;
import com.shopmicro.auth.dto.RegisterRequest;
import com.shopmicro.auth.entity.User;
import com.shopmicro.auth.enums.ERole;
import com.shopmicro.auth.repository.UserRepository;
import com.shopmicro.common.exception.BadRequestException;
import com.shopmicro.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  @Transactional
  public AuthResponse register(RegisterRequest req) {
    if (userRepository.existsByEmail(req.email())) {
      throw new ConflictException("Email đã được đăng ký");
    }
    User user = User.builder()
        .email(req.email())
        .password(passwordEncoder.encode(req.password()))
        .fullName(req.fullName())
        .role(ERole.USER)
        .build();
    userRepository.save(user);
    log.info("Đăng ký user mới: {}", user.getEmail());
    return toResponse(user);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest req) {
    User user = userRepository.findByEmail(req.email())
        .orElseThrow(() -> new BadRequestException("Email hoặc mật khẩu không đúng"));
    if (!passwordEncoder.matches(req.password(), user.getPassword())) {
      throw new BadRequestException("Email hoặc mật khẩu không đúng");
    }
    return toResponse(user);
  }

  private AuthResponse toResponse(User user) {
    String token = jwtService.generateToken(user);
    return new AuthResponse(user.getId(), user.getEmail(), user.getFullName(),
        user.getRole().name(), token);
  }
}
