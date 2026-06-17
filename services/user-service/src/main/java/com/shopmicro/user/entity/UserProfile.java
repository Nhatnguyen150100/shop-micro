package com.shopmicro.user.entity;

import com.shopmicro.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Hồ sơ người dùng. Lưu ý: userId đến từ auth-service (qua JWT),
 * user-service KHÔNG lưu mật khẩu — mỗi service chỉ giữ dữ liệu thuộc về mình.
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile extends BaseEntity {

  @Column(nullable = false, unique = true)
  private UUID userId;

  private String email;
  private String fullName;
  private String phone;
  private String address;
}
