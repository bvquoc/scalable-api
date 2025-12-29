package com.project.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for load balancer probes.
 * Public endpoint (no authentication required).
 */
@RestController
@RequestMapping("/actuator")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> info() {
        Map<String, String> response = new HashMap<>();
        response.put("application", "Scalable API");
        response.put("version", "1.0.0");
        response.put("description", "Spring Boot API with Redis caching and rate limiting");

        return ResponseEntity.ok(response);
    }
}
