package com.shopmicro.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Khớp với cấu trúc BaseResponse mà các service khác trả về,
 * để Feign bóc được trường "data".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiResult<T>(boolean success, String message, T data) {
}
