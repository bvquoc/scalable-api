package com.project.security.authentication;

import com.project.domain.model.ApiKey;
import com.project.infrastructure.cache.ApiKeyCacheService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Authentication filter for API key-based authentication.
 *
 * Flow:
 * 1. Extract API key from X-API-Key header
 * 2. Hash the API key with SHA-256
 * 3. Lookup hashed key in cache/database
 * 4. Validate key (active, not expired)
 * 5. Set authentication in SecurityContext
 *
 * Header format: X-API-Key: <plain-api-key>
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyCacheService apiKeyCacheService;

    public ApiKeyAuthenticationFilter(ApiKeyCacheService apiKeyCacheService) {
        this.apiKeyCacheService = apiKeyCacheService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. Extract API key from header
        String apiKeyPlain = request.getHeader(API_KEY_HEADER);

        if (apiKeyPlain == null || apiKeyPlain.isEmpty()) {
            log.debug("No API key found in request headers");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2. Hash the API key
            String keyHash = hashApiKey(apiKeyPlain);

            // 3. Lookup API key
            Optional<ApiKey> apiKeyOpt = apiKeyCacheService.findByKeyHash(keyHash);

            if (apiKeyOpt.isEmpty()) {
                log.warn("Invalid API key attempted: {}", maskApiKey(apiKeyPlain));
                filterChain.doFilter(request, response);
                return;
            }

            ApiKey apiKey = apiKeyOpt.get();

            // 4. Validate API key
            if (!isValid(apiKey)) {
                log.warn("Invalid/expired API key: {} (user: {})",
                    apiKey.getName(), apiKey.getUserId());
                filterChain.doFilter(request, response);
                return;
            }

            // 5. Set authentication in SecurityContext
            ApiKeyAuthentication authentication = new ApiKeyAuthentication(apiKey);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Authenticated request with API key: {} (user: {})",
                apiKey.getName(), apiKey.getUserId());

            // Update last used timestamp asynchronously (don't block request)
            updateLastUsedAsync(keyHash);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Error during API key authentication: {}", e.getMessage(), e);
            filterChain.doFilter(request, response);
        } finally {
            // Clear security context after request
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Hash API key with SHA-256.
     */
    private String hashApiKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return new String(Hex.encode(hash));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Validate API key is active and not expired.
     */
    private boolean isValid(ApiKey apiKey) {
        if (!Boolean.TRUE.equals(apiKey.getIsActive())) {
            return false;
        }

        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        return true;
    }

    /**
     * Update last used timestamp asynchronously.
     */
    private void updateLastUsedAsync(String keyHash) {
        // TODO: Move to async executor to avoid blocking request
        try {
            apiKeyCacheService.updateLastUsedAt(keyHash);
        } catch (Exception e) {
            log.error("Failed to update last_used_at: {}", e.getMessage());
        }
    }

    /**
     * Mask API key for logging (show first 4 chars only).
     */
    private String maskApiKey(String apiKey) {
        if (apiKey.length() <= 4) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****";
    }
}
