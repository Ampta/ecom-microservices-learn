package com.microservices.orderservice.service;

import com.microservices.orderservice.client.ProductClient;
import com.microservices.orderservice.dto.OrderRequestDto;
import com.microservices.orderservice.dto.OrderResponseDto;
import com.microservices.orderservice.dto.ProductResponseDto;
import com.microservices.orderservice.entity.OrderMaster;
import com.microservices.orderservice.entity.OrderStatus;
import com.microservices.orderservice.exception.OrderNotFoundException;
import com.microservices.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private OrderService orderService;

    private OrderRequestDto orderRequest;
    private ProductResponseDto mockProduct;
    private OrderMaster savedOrder;

    @BeforeEach
    void setUp() {
        orderRequest = OrderRequestDto.builder()
                .userId(101L)
                .productId(1L)
                .quantity(2)
                .build();

        mockProduct = ProductResponseDto.builder()
                .id(1L)
                .name("MacBook Pro M3")
                .price(new BigDecimal("2000.00"))
                .stockQuantity(10)
                .build();

        savedOrder = OrderMaster.builder()
                .id(501L)
                .userId(101L)
                .productId(1L)
                .quantity(2)
                .totalPrice(new BigDecimal("4000.00"))
                .orderStatus(OrderStatus.CREATED)
                .build();
    }

    @Test
    @DisplayName("createOrder() - Should fetch product via Feign, calculate total price, and persist CREATED order")
    void createOrder_Success() {
        // ARRANGE
        when(productClient.getProductById(1L)).thenReturn(mockProduct);
        when(orderRepository.save(any(OrderMaster.class))).thenReturn(savedOrder);

        // ACT
        OrderResponseDto response = orderService.createOrder(orderRequest);

        // ASSERT
        assertNotNull(response);
        assertEquals(OrderStatus.CREATED, response.getOrderStatus());
        assertEquals(new BigDecimal("4000.00"), response.getTotalPrice());

        // Verify totalPrice calculation: price (2000) * quantity (2) = 4000
        ArgumentCaptor<OrderMaster> captor = ArgumentCaptor.forClass(OrderMaster.class);
        verify(orderRepository).save(captor.capture());
        assertEquals(new BigDecimal("4000.00"), captor.getValue().getTotalPrice());
        assertEquals(OrderStatus.CREATED, captor.getValue().getOrderStatus());

        verify(productClient, times(1)).getProductById(1L);
    }

    @Test
    @DisplayName("createOrder() - Should mark order as FAILED when stock is insufficient")
    void createOrder_InsufficientStock_MarksOrderFailed() {
        // ARRANGE: Available stock is 1, but order requests 2
        mockProduct.setStockQuantity(1);
        when(productClient.getProductById(1L)).thenReturn(mockProduct);
        when(orderRepository.save(any(OrderMaster.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ACT
        OrderResponseDto response = orderService.createOrder(orderRequest);

        // ASSERT
        assertNotNull(response);
        assertEquals(OrderStatus.FAILED, response.getOrderStatus());
        assertTrue(response.getFailureReason().contains("Insufficient stock"));
        assertEquals(BigDecimal.ZERO, response.getTotalPrice());

        verify(orderRepository).save(any(OrderMaster.class));
    }

    @Test
    @DisplayName("createOrderFallback() - Should mark order as FAILED when circuit breaker triggers")
    void createOrderFallback_Execution_MarksOrderFailed() {
        // ARRANGE: Simulated timeout or connection failure
        Throwable timeoutException = new RuntimeException("Read timed out executing GET http://product-service/api/v1/products/1");
        when(orderRepository.save(any(OrderMaster.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ACT
        OrderResponseDto response = orderService.createOrderFallback(orderRequest, timeoutException);

        // ASSERT
        assertNotNull(response);
        assertEquals(OrderStatus.FAILED, response.getOrderStatus());
        assertTrue(response.getFailureReason().contains("Product Service unavailable or timed out"));
        assertEquals(BigDecimal.ZERO, response.getTotalPrice());

        verify(orderRepository, times(1)).save(any(OrderMaster.class));
    }

    @Test
    @DisplayName("getOrderById() - Should return order when ID exists")
    void getOrderById_Success() {
        when(orderRepository.findById(501L)).thenReturn(Optional.of(savedOrder));

        OrderResponseDto response = orderService.getOrderById(501L);

        assertNotNull(response);
        assertEquals(501L, response.getId());
        assertEquals(OrderStatus.CREATED, response.getOrderStatus());
        verify(orderRepository, times(1)).findById(501L);
    }

    @Test
    @DisplayName("getOrderById() - Should throw OrderNotFoundException when ID does not exist")
    void getOrderById_NotFound_ThrowsException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(999L));
        verify(orderRepository, times(1)).findById(999L);
    }
}

