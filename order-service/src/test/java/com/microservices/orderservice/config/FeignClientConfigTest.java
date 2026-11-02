package com.microservices.orderservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FeignClientConfig Unit Tests")
class FeignClientConfigTest {

    private FeignClientConfig feignClientConfig;
    private RequestTemplate requestTemplate;

    @BeforeEach
    void setUp() {
        feignClientConfig = new FeignClientConfig();
        requestTemplate = new RequestTemplate();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Should extract Bearer token from RequestContextHolder and attach to Feign RequestTemplate")
    void apply_WithAuthHeader_AttachesToFeignRequest() {
        // ARRANGE: Setup incoming mock HTTP request with JWT Bearer header
        MockHttpServletRequest incomingRequest = new MockHttpServletRequest();
        incomingRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-jwt-token-12345");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(incomingRequest));

        // ACT: Execute interceptor
        RequestInterceptor interceptor = feignClientConfig.jwtFeignRequestInterceptor();
        interceptor.apply(requestTemplate);

        // ASSERT: Header is attached to outgoing request
        Collection<String> authHeaders = requestTemplate.headers().get(HttpHeaders.AUTHORIZATION);
        assertNotNull(authHeaders);
        assertTrue(authHeaders.contains("Bearer test-jwt-token-12345"));
    }

    @Test
    @DisplayName("Should gracefully handle request when no Authorization header is present")
    void apply_WithoutAuthHeader_DoesNotAttachHeader() {
        // ARRANGE: Setup incoming mock HTTP request with NO Authorization header
        MockHttpServletRequest incomingRequest = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(incomingRequest));

        // ACT
        RequestInterceptor interceptor = feignClientConfig.jwtFeignRequestInterceptor();
        interceptor.apply(requestTemplate);

        // ASSERT
        assertNull(requestTemplate.headers().get(HttpHeaders.AUTHORIZATION));
    }

    @Test
    @DisplayName("Should gracefully handle when RequestContextHolder has null attributes")
    void apply_WithNullRequestContext_DoesNotThrowException() {
        // ARRANGE: Ensure no request attributes are bound
        RequestContextHolder.resetRequestAttributes();

        // ACT & ASSERT: Must not throw NullPointerException
        RequestInterceptor interceptor = feignClientConfig.jwtFeignRequestInterceptor();
        assertDoesNotThrow(() -> interceptor.apply(requestTemplate));
        assertNull(requestTemplate.headers().get(HttpHeaders.AUTHORIZATION));
    }
}

