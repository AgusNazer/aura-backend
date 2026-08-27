package com.aura_api.aura_farmer.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Aplica el límite únicamente a la creación de órdenes
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/orders/**")
                .excludePathPatterns("/api/v1/webhooks/**"); // Nunca limitar los webhooks de Mercado Pago
    }
}