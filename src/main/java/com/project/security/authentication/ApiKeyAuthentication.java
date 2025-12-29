package com.project.security.authentication;

import com.project.domain.model.ApiKey;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security Authentication implementation for API keys.
 * Represents an authenticated API key with associated authorities.
 */
public class ApiKeyAuthentication implements Authentication {

    private final ApiKey apiKey;
    private final List<GrantedAuthority> authorities;
    private boolean authenticated;

    public ApiKeyAuthentication(ApiKey apiKey) {
        this.apiKey = apiKey;
        this.authorities = extractAuthorities(apiKey);
        this.authenticated = true;
    }

    public ApiKeyAuthentication(String keyHash) {
        this.apiKey = null;
        this.authorities = Collections.emptyList();
        this.authenticated = false;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return apiKey != null ? apiKey.getKeyHash() : null;
    }

    @Override
    public Object getDetails() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return apiKey != null ? apiKey.getUserId() : null;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean authenticated) throws IllegalArgumentException {
        this.authenticated = authenticated;
    }

    @Override
    public String getName() {
        return apiKey != null ? apiKey.getName() : "anonymous";
    }

    /**
     * Get the API key object.
     */
    public ApiKey getApiKey() {
        return apiKey;
    }

    /**
     * Extract granted authorities from API key scopes.
     * Scopes are converted to Spring Security authorities.
     */
    private List<GrantedAuthority> extractAuthorities(ApiKey apiKey) {
        if (apiKey == null || apiKey.getScopes() == null) {
            return Collections.emptyList();
        }

        return java.util.Arrays.stream(apiKey.getScopes())
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .collect(Collectors.toList());
    }
}
