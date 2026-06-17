package com.shopmicro.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception nghiệp vụ cơ sở, gắn sẵn mã HTTP để GlobalExceptionHandler dùng lại.
 */
@Getter
public class AppException extends RuntimeException {
  private final HttpStatus status;

  public AppException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }
}
