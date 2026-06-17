package com.shopmicro.common.exception;

import com.shopmicro.common.response.BaseResponse;
import com.shopmicro.common.response.ResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Bộ bắt lỗi toàn cục dùng chung. Mỗi service chỉ cần import common-lib là có.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AppException.class)
  public ResponseEntity<BaseResponse<Void>> handleAppException(AppException ex) {
    return ResponseBuilder.error(ex.getStatus(), ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<BaseResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
    String msg = ex.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining("; "));
    return ResponseBuilder.error(HttpStatus.BAD_REQUEST, msg);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<BaseResponse<Void>> handleGeneric(Exception ex) {
    return ResponseBuilder.error(HttpStatus.INTERNAL_SERVER_ERROR,
        "Lỗi hệ thống: " + ex.getMessage());
  }
}
