package com.shopmicro.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Định dạng response chuẩn dùng chung cho TẤT CẢ các service.
 * Giúp client luôn nhận về cùng một cấu trúc JSON, dù gọi service nào.
 */
@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {
  private boolean success;
  private int status;
  private String message;
  private T data;
  @Builder.Default
  private Instant timestamp = Instant.now();
}
