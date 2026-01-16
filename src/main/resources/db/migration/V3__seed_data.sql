-- V3__seed_data.sql
-- Seed data for local development and testing

-- Insert test user
INSERT INTO users (email, username, full_name, status, created_at, updated_at)
VALUES 
    ('test@example.com', 'testuser', 'Test User', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- Insert test API key
-- Plain API key: test-api-key-local-dev
-- Hash (SHA-256): 489545ff5724037835fceb90b6533abcd4b7c23c25e58fddc6f433e43278b078
-- Rate limit tier: PREMIUM (1000 requests/minute)
INSERT INTO api_keys (key_hash, user_id, name, scopes, rate_limit_tier, is_active, created_at, updated_at)
SELECT 
    '489545ff5724037835fceb90b6533abcd4b7c23c25e58fddc6f433e43278b078',
    u.id,
    'Local Development Key',
    ARRAY['read', 'write']::TEXT[],
    'PREMIUM',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'test@example.com'
ON CONFLICT (key_hash) DO NOTHING;

