package com.aura_api.aura_farmer.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private RateLimitInterceptor rateLimitInterceptor;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Primera petición: debe permitir el paso y setear TTL de 1 minuto en Redis")
    void firstRequestShouldSetExpireAndAllow() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("181.44.12.34");
        when(request.getMethod()).thenReturn("POST");
        when(valueOperations.increment("rate:limit:181.44.12.34:POST")).thenReturn(1L);

        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(redisTemplate).expire(eq("rate:limit:181.44.12.34:POST"), any(Duration.class));
    }

    @Test
    @DisplayName("Petición 5: debe permitir el paso sin resetear TTL")
    void fifthRequestShouldAllowWithoutSettingExpire() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("181.44.12.34");
        when(request.getMethod()).thenReturn("POST");
        when(valueOperations.increment("rate:limit:181.44.12.34:POST")).thenReturn(5L);

        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Petición 6: debe bloquear con HTTP 429 Too Many Requests")
    void sixthRequestShouldBlockWith429() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(request.getHeader("X-Forwarded-For")).thenReturn("181.44.12.34");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        when(valueOperations.increment("rate:limit:181.44.12.34:POST")).thenReturn(6L);
        when(response.getWriter()).thenReturn(printWriter);

        boolean result = rateLimitInterceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        assertTrue(stringWriter.toString().contains("Demasiadas solicitudes"));
    }
}