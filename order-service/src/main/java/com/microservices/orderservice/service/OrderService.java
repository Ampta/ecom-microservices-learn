package com.microservices.orderservice.service;

import com.microservices.orderservice.client.ProductClient;
import com.microservices.orderservice.dto.OrderRequestDto;
import com.microservices.orderservice.dto.OrderResponseDto;
import com.microservices.orderservice.dto.ProductResponseDto;
import com.microservices.orderservice.entity.OrderMaster;
import com.microservices.orderservice.entity.OrderStatus;
import com.microservices.orderservice.exception.OrderNotFoundException;
import com.microservices.orderservice.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    /**
     * Creates an order by invoking ProductClient via Feign to fetch product details.
     * Protected by Resilience4j Circuit Breaker 'productService'.
     */
    @Transactional
    @CircuitBreaker(name = "productService", fallbackMethod = "createOrderFallback")
    public OrderResponseDto createOrder(OrderRequestDto request) {
        log.info("Processing order for userId: {}, productId: {}, quantity: {}",
                request.getUserId(), request.getProductId(), request.getQuantity());

        // 1. Fetch product details dynamically via Feign & Eureka
        ProductResponseDto product = productClient.getProductById(request.getProductId());

        if (product == null) {
            log.warn("Product not found with id: {}", request.getProductId());
            return persistFailedOrder(request, "Product not found with id: " + request.getProductId());
        }

        // 2. Validate stock availability
        if (product.getStockQuantity() == null || product.getStockQuantity() < request.getQuantity()) {
            String reason = String.format("Insufficient stock for productId: %d. Available: %d, Requested: %d",
                    request.getProductId(),
                    product.getStockQuantity() != null ? product.getStockQuantity() : 0,
                    request.getQuantity());
            log.warn(reason);
            return persistFailedOrder(request, reason);
        }

        // 3. Calculate total price (price * quantity)
        BigDecimal totalPrice = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        // 4. Build and persist CREATED order
        OrderMaster order = OrderMaster.builder()
                .userId(request.getUserId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalPrice(totalPrice)
                .orderStatus(OrderStatus.CREATED)
                .build();

        OrderMaster savedOrder = orderRepository.save(order);
        log.info("Order placed successfully with ID: {}, Total: {}", savedOrder.getId(), savedOrder.getTotalPrice());

        return mapToResponseDto(savedOrder);
    }

    /**
     * Resilience4j Circuit Breaker Fallback Method.
     * Triggered when Product Service is unreachable, throws an exception, or times out.
     * Marks the order status as FAILED and gracefully returns an informative response.
     */
    @Transactional
    public OrderResponseDto createOrderFallback(OrderRequestDto request, Throwable throwable) {
        String reason = "Product Service unavailable or timed out: " + throwable.getMessage();
        log.error("Circuit breaker triggered for productId: {}. Reason: {}", request.getProductId(), reason);
        return persistFailedOrder(request, reason);
    }

    private OrderResponseDto persistFailedOrder(OrderRequestDto request, String failureReason) {
        OrderMaster failedOrder = OrderMaster.builder()
                .userId(request.getUserId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalPrice(BigDecimal.ZERO)
                .orderStatus(OrderStatus.FAILED)
                .failureReason(failureReason)
                .build();

        OrderMaster saved = orderRepository.save(failedOrder);
        return mapToResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long id) {
        OrderMaster order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        return mapToResponseDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    private OrderResponseDto mapToResponseDto(OrderMaster order) {
        return OrderResponseDto.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .totalPrice(order.getTotalPrice())
                .orderStatus(order.getOrderStatus())
                .failureReason(order.getFailureReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}

