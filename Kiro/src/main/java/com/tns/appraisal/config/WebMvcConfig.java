package com.tns.appraisal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for CORS and custom message converters.
 * Enables frontend-backend communication for the Angular SPA.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Configures CORS to allow the Angular frontend to communicate with the backend.
     * - Allows credentials (cookies/session)
     * - Permits standard HTTP methods
     * - Allows common headers
     * - Configured for development environment (adjust origins for production)
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            // Allow Angular dev server (adjust for production)
            .allowedOrigins("http://localhost:4200")
            
            // Allow credentials (session cookies)
            .allowCredentials(true)
            
            // Allow standard HTTP methods
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            
            // Allow common headers
            .allowedHeaders("*")
            
            // Cache preflight response for 1 hour
            .maxAge(3600);
    }
}
