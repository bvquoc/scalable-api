package com.project.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response DTO.
 * Used for consistent error formatting across all API endpoints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String error;
    private String message;
    private int status;
    private LocalDateTime timestamp;
    private String path;
    private Map<String, Object> details;

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(String error, String message, int status) {
        this.error = error;
        this.message = message;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ErrorResponse response = new ErrorResponse();

        public Builder error(String error) {
            response.error = error;
            return this;
        }

        public Builder message(String message) {
            response.message = message;
            return this;
        }

        public Builder status(int status) {
            response.status = status;
            return this;
        }

        public Builder path(String path) {
            response.path = path;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            response.details = details;
            return this;
        }

        public ErrorResponse build() {
            return response;
        }
    }

    // Getters and setters
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
