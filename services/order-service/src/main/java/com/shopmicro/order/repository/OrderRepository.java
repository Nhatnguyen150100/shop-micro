package com.shopmicro.order.repository;

import com.shopmicro.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
  List<Order> findByUserId(UUID userId);
}
