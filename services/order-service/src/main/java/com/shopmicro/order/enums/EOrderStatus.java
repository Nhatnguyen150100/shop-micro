package com.shopmicro.order.enums;

public enum EOrderStatus {
  PENDING,    // vừa tạo, đang chờ thanh toán + trừ kho
  CONFIRMED,  // hoàn tất Saga thành công
  CANCELLED   // Saga thất bại (đã bù trừ nếu cần)
}
