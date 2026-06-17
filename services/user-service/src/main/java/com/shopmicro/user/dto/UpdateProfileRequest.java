package com.shopmicro.user.dto;

public record UpdateProfileRequest(
    String fullName,
    String phone,
    String address) {
}
