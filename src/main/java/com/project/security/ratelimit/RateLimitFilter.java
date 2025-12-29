package com.project.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.model.ApiKey;
import com.project.security.authentication.ApiKeyAuthentication;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Rate limiting filter using Redis-backed token bucket.
 * Runs after authentication filter.
 *
 * Response headers:
 * - X-RateLimit-Limit: Maximum requests per window
 * - X-RateLimit-Remaining: Remaining requests in current window
 * - X-RateLimit-Reset: Seconds until window reset
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Get authentication from SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Only rate limit authenticated API key requests
        if (authentication == null || !(authentication instanceof ApiKeyAuthentication)) {
            filterChain.doFilter(request, response);
            return;
        }

        ApiKeyAuthentication apiKeyAuth = (ApiKeyAuthentication) authentication;
        ApiKey apiKey = apiKeyAuth.getApiKey();

        if (apiKey == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check rate limit
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimit(apiKey);

        // Add rate limit headers
        addRateLimitHeaders(response, result, apiKey);

        // If rate limit exceeded, return 429 Too Many Requests
        if (!result.isAllowed()) {
            sendRateLimitExceededResponse(response, result);
            return;
        }

        // Rate limit OK, continue with request
        filterChain.doFilter(request, response);
    }

    /**
     * Add rate limit headers to response.
     */
    private void addRateLimitHeaders(
            HttpServletResponse response,
            RateLimitService.RateLimitResult result,
            ApiKey apiKey) {

        int limit = getRateLimitForTier(apiKey.getRateLimitTier());

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.getResetSeconds()));
    }

    /**
     * Send 429 Too Many Requests response.
     */
    private void sendRateLimitExceededResponse(
            HttpServletResponse response,
            RateLimitService.RateLimitResult result) throws IOException {

        response.setStatus(429); // Too Many Requests
        response.setContentType("application/json");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "rate_limit_exceeded");
        errorResponse.put("message", "Rate limit exceeded. Please retry after the reset time.");
        errorResponse.put("retryAfter", result.getResetSeconds());

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Get rate limit for tier.
     */
    private int getRateLimitForTier(ApiKey.RateLimitTier tier) {
        return switch (tier) {
            case BASIC -> 60;
            case STANDARD -> 300;
            case PREMIUM -> 1000;
            case UNLIMITED -> Integer.MAX_VALUE;
        };
    }
}
