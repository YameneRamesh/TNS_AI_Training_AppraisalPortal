package com.tns.appraisal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * Security configuration for session-based authentication with role-based access control.
 * Configures Spring Security for the Employee Appraisal Cycle application.
 */
@Configuration
@EnableWebSecurity
// @EnableMethodSecurity // Disabled for development - re-enable when authentication is implemented
public class SecurityConfig {

    /**
     * Configures HTTP security with session-based authentication.
     * - Disables CSRF for POC environment
     * - Uses HTTP (not HTTPS) for POC
     * - Sets session timeout to 15 minutes (configured in application.properties)
     * - Defines public and protected endpoints
     * - Handles session expiration with 401 response
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS - delegates to WebMvcConfig.addCorsMappings()
            .cors(Customizer.withDefaults())

            // Disable CSRF for POC environment
            .csrf(csrf -> csrf.disable())
            
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // All endpoints are publicly accessible for development testing
                .anyRequest().permitAll()
            )
            
            // Configure session management with 15-minute timeout
            .sessionManagement(session -> session
                // Allow only one session per user
                .maximumSessions(1)
                // Don't prevent login if max sessions reached (invalidate old session)
                .maxSessionsPreventsLogin(false)
                // Handle expired sessions with 401 response
                .expiredSessionStrategy(event -> {
                    var response = event.getResponse();
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"timestamp\":\"" + java.time.Instant.now() + "\"," +
                        "\"status\":401," +
                        "\"error\":\"Unauthorized\"," +
                        "\"message\":\"Session expired due to inactivity\"," +
                        "\"path\":\"" + event.getRequest().getRequestURI() + "\"}"
                    );
                })
            )
            
            // Configure logout
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(200);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"timestamp\":\"" + java.time.Instant.now() + "\"," +
                        "\"status\":200," +
                        "\"message\":\"Logout successful\"}"
                    );
                })
            )
            
            // Return 401 for unauthenticated requests
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"timestamp\":\"" + java.time.Instant.now() + "\"," +
                        "\"status\":401," +
                        "\"error\":\"Unauthorized\"," +
                        "\"message\":\"Authentication required\"," +
                        "\"path\":\"" + request.getRequestURI() + "\"}"
                    );
                })
            );
        
        return http.build();
    }

    /**
     * Password encoder using BCrypt hashing algorithm.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication manager for handling authentication requests.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Session event publisher for tracking session lifecycle events.
     * Required for session timeout handling.
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
