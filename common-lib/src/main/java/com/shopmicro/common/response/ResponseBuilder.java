package com.shopmicro.common.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Tiện ích tạo ResponseEntity chuẩn hoá (kế thừa ý tưởng từ boilerplate gốc).
 */
public final class ResponseBuilder {

  private ResponseBuilder() {
  }

  public static <T> ResponseEntity<BaseResponse<T>> success(String message, T data) {
    return build(HttpStatus.OK, true, message, data);
  }

  public static ResponseEntity<BaseResponse<Void>> success(String message) {
    return build(HttpStatus.OK, true, message, null);
  }

  public static <T> ResponseEntity<BaseResponse<T>> created(String message, T data) {
    return build(HttpStatus.CREATED, true, message, data);
  }

  public static <T> ResponseEntity<BaseResponse<T>> error(HttpStatus status, String message) {
    return build(status, false, message, null);
  }

  public static <T> ResponseEntity<BaseResponse<T>> build(HttpStatus status, boolean success, String message, T data) {
    BaseResponse<T> body = BaseResponse.<T>builder()
        .success(success)
        .status(status.value())
        .message(message)
        .data(data)
        .build();
    return ResponseEntity.status(status).body(body);
  }
}
