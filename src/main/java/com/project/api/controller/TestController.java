package com.project.api.controller;

import com.project.domain.model.ApiKey;
import com.project.security.authentication.ApiKeyAuthentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Test controller for authentication and rate limiting.
 * Protected endpoint (requires valid API key).
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/protected")
    public ResponseEntity<Map<String, Object>> protectedEndpoint(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Successfully authenticated");
        response.put("timestamp", LocalDateTime.now());

        if (authentication instanceof ApiKeyAuthentication apiKeyAuth) {
            ApiKey apiKey = apiKeyAuth.getApiKey();
            response.put("apiKeyName", apiKey.getName());
            response.put("userId", apiKey.getUserId());
            response.put("rateLimitTier", apiKey.getRateLimitTier());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/echo")
    public ResponseEntity<Map<String, String>> echo() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Echo successful");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }
}
