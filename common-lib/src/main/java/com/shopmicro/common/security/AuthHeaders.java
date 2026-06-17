package com.shopmicro.common.security;

/**
 * Tên các header định danh người dùng mà API Gateway gắn vào request
 * SAU KHI đã xác thực JWT thành công, rồi chuyển tiếp xuống các service nội bộ.
 *
 * <p>Mô hình "Gateway-centric": Gateway xác thực 1 lần, service nội bộ tin tưởng
 * các header này (service nội bộ không expose ra ngoài internet).
 */
public final class AuthHeaders {

  private AuthHeaders() {
  }

  public static final String USER_ID = "X-User-Id";
  public static final String USER_EMAIL = "X-User-Email";
  public static final String USER_ROLE = "X-User-Role";
}
