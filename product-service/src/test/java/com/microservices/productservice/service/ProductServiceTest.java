package com.microservices.productservice.service;

import com.microservices.productservice.dto.ProductRequestDto;
import com.microservices.productservice.dto.ProductResponseDto;
import com.microservices.productservice.entity.Product;
import com.microservices.productservice.exception.ResourceNotFoundException;
import com.microservices.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private ProductRequestDto requestDto;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id(1L)
                .name("MacBook Pro M3")
                .description("16-inch, 36GB RAM, 1TB SSD")
                .price(new BigDecimal("2499.99"))
                .stockQuantity(15)
                .build();

        requestDto = ProductRequestDto.builder()
                .name("MacBook Pro M3")
                .description("16-inch, 36GB RAM, 1TB SSD")
                .price(new BigDecimal("2499.99"))
                .stockQuantity(15)
                .build();
    }

    @Test
    @DisplayName("createProduct() - Should successfully persist product and return ProductResponseDto")
    void createProduct_Success() {
        // ARRANGE
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // ACT
        ProductResponseDto response = productService.createProduct(requestDto);

        // ASSERT
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("MacBook Pro M3", response.getName());
        assertEquals(new BigDecimal("2499.99"), response.getPrice());
        assertEquals(15, response.getStockQuantity());

        // VERIFY
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("getProductById() - Should return product when ID exists")
    void getProductById_Success() {
        // ARRANGE
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // ACT
        ProductResponseDto response = productService.getProductById(1L);

        // ASSERT
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("MacBook Pro M3", response.getName());
        assertEquals(15, response.getStockQuantity());

        // VERIFY
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getProductById() - Should throw ResourceNotFoundException when product does not exist")
    void getProductById_NotFound_ThrowsException() {
        // ARRANGE
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.getProductById(999L)
        );

        assertEquals("Product not found with id: 999", exception.getMessage());
        verify(productRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("getAllProducts() - Should return list of all products in catalog")
    void getAllProducts_Success() {
        // ARRANGE
        Product product2 = Product.builder()
                .id(2L)
                .name("iPhone 16 Pro")
                .description("256GB Black Titanium")
                .price(new BigDecimal("1199.00"))
                .stockQuantity(50)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(testProduct, product2));

        // ACT
        List<ProductResponseDto> products = productService.getAllProducts();

        // ASSERT
        assertNotNull(products);
        assertEquals(2, products.size());
        assertEquals("MacBook Pro M3", products.get(0).getName());
        assertEquals("iPhone 16 Pro", products.get(1).getName());

        verify(productRepository, times(1)).findAll();
    }
}

