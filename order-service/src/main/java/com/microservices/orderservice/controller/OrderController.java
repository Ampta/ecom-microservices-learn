package com.microservices.orderservice.controller;

import com.microservices.orderservice.dto.OrderRequestDto;
import com.microservices.orderservice.dto.OrderResponseDto;
import com.microservices.orderservice.entity.OrderStatus;
import com.microservices.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * POST /api/v1/orders
     * Places a new order. Interacts with Product Service via OpenFeign and Resilience4j.
     */
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto request) {
        OrderResponseDto response = orderService.createOrder(request);
        
        // If order failed due to stock/fallback, return 422 Unprocessable Entity or 200 with status FAILED
        HttpStatus status = (response.getOrderStatus() == OrderStatus.CREATED) 
                ? HttpStatus.CREATED 
                : HttpStatus.SERVICE_UNAVAILABLE;
                
        return ResponseEntity.status(status).body(response);
    }

    /**
     * GET /api/v1/orders/{id}
     * Retrieves an order by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable Long id) {
        OrderResponseDto order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    /**
     * GET /api/v1/orders
     * Lists all orders.
     */
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() {
        List<OrderResponseDto> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }
}

