package com.microservices.orderservice.repository;

import com.microservices.orderservice.entity.OrderMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderMaster, Long> {

    List<OrderMaster> findByUserId(Long userId);
}

