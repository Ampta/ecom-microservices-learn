package com.microservices.productservice.service;

import com.microservices.productservice.dto.ProductRequestDto;
import com.microservices.productservice.dto.ProductResponseDto;
import com.microservices.productservice.entity.Product;
import com.microservices.productservice.exception.ResourceNotFoundException;
import com.microservices.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * Creates a new product and persists it to MySQL.
     */
    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto request) {
        log.info("Creating new product: {}", request.getName());

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getId());

        return mapToResponseDto(savedProduct);
    }

    /**
     * Fetches product details by ID (used by Order Service for inventory checks).
     */
    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id) {
        log.info("Fetching product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        return mapToResponseDto(product);
    }

    /**
     * Retrieves all products in the catalog.
     */
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts() {
        log.info("Retrieving all products");

        return productRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    /**
     * Helper to map Entity to Response DTO.
     */
    private ProductResponseDto mapToResponseDto(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}

