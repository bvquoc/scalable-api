package com.project.security.config;

import com.project.security.authentication.ApiKeyAuthenticationFilter;
import com.project.security.ratelimit.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for API key-based authentication.
 *
 * Security Chain:
 * 1. ApiKeyAuthenticationFilter - Extract and validate API key
 * 2. RateLimitFilter - Check rate limits
 * 3. Authorization - Require authentication for protected endpoints
 *
 * Public endpoints: /actuator/health, /actuator/info, /actuator/metrics, /actuator/prometheus, /swagger-ui/**, /v3/api-docs/**
 * Protected endpoints: All others require valid API key
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(
            ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
            RateLimitFilter rateLimitFilter) {
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (stateless API with token-based auth)
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless session (no cookies, no server-side sessions)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public actuator endpoints (for monitoring and health checks)
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/actuator/metrics",
                    "/actuator/metrics/**",
                    "/actuator/prometheus"
                ).permitAll()

                // Swagger UI and OpenAPI endpoints (public for documentation access)
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**"
                ).permitAll()

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )

            // Add custom filters
            // 1. Authentication filter (before default auth filter)
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // 2. Rate limiting filter (after authentication)
            .addFilterAfter(rateLimitFilter, ApiKeyAuthenticationFilter.class);

        return http.build();
    }
}
