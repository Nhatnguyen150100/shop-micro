package com.shopmicro.user.service;

import com.shopmicro.user.dto.UpdateProfileRequest;
import com.shopmicro.user.entity.UserProfile;
import com.shopmicro.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

  private final UserProfileRepository repository;

  /**
   * Lấy hồ sơ theo userId; nếu chưa có thì tạo mới (lazy provisioning)
   * dựa trên thông tin định danh mà Gateway truyền xuống.
   */
  @Transactional
  public UserProfile getOrCreate(UUID userId, String email) {
    return repository.findByUserId(userId)
        .orElseGet(() -> repository.save(
            UserProfile.builder().userId(userId).email(email).build()));
  }

  @Transactional
  public UserProfile update(UUID userId, String email, UpdateProfileRequest req) {
    UserProfile profile = getOrCreate(userId, email);
    if (req.fullName() != null) profile.setFullName(req.fullName());
    if (req.phone() != null) profile.setPhone(req.phone());
    if (req.address() != null) profile.setAddress(req.address());
    return repository.save(profile);
  }
}
