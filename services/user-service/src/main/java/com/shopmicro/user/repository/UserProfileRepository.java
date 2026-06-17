package com.shopmicro.user.repository;

import com.shopmicro.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
  Optional<UserProfile> findByUserId(UUID userId);
}
