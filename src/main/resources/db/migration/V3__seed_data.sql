-- V3__seed_data.sql
-- Comprehensive demo data for local development and testing
-- Includes users, products, orders, and API keys for all demo scenarios

-- ============================================================================
-- USERS
-- ============================================================================
INSERT INTO users (email, username, full_name, status, created_at, updated_at)
VALUES 
    ('test@example.com', 'testuser', 'Test User', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('alice@example.com', 'alice', 'Alice Johnson', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bob@example.com', 'bob', 'Bob Smith', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('charlie@example.com', 'charlie', 'Charlie Brown', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('diana@example.com', 'diana', 'Diana Prince', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('inactive@example.com', 'inactive', 'Inactive User', 'INACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- ============================================================================
-- API KEYS
-- ============================================================================
-- Test API key: test-api-key-local-dev
-- Hash (SHA-256): 489545ff5724037835fceb90b6533abcd4b7c23c25e58fddc6f433e43278b078
INSERT INTO api_keys (key_hash, user_id, name, scopes, rate_limit_tier, is_active, created_at, updated_at)
SELECT 
    '489545ff5724037835fceb90b6533abcd4b7c23c25e58fddc6f433e43278b078',
    u.id,
    'Local Development Key (PREMIUM)',
    ARRAY['read', 'write']::TEXT[],
    'PREMIUM',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'test@example.com'
ON CONFLICT (key_hash) DO NOTHING;

-- Additional API keys for different rate limit tiers
INSERT INTO api_keys (key_hash, user_id, name, scopes, rate_limit_tier, is_active, created_at, updated_at)
SELECT 
    'basic-key-hash-' || u.id,
    u.id,
    'Basic Tier Key',
    ARRAY['read']::TEXT[],
    'BASIC',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'alice@example.com'
ON CONFLICT (key_hash) DO NOTHING;

INSERT INTO api_keys (key_hash, user_id, name, scopes, rate_limit_tier, is_active, created_at, updated_at)
SELECT 
    'standard-key-hash-' || u.id,
    u.id,
    'Standard Tier Key',
    ARRAY['read', 'write']::TEXT[],
    'STANDARD',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'bob@example.com'
ON CONFLICT (key_hash) DO NOTHING;

-- ============================================================================
-- PRODUCTS
-- ============================================================================
INSERT INTO products (name, description, sku, price, stock_quantity, category, is_active, created_at, updated_at)
VALUES 
    -- Electronics
    ('Laptop Pro 15', 'High-performance laptop with 16GB RAM, 512GB SSD', 'LAP-001', 1299.99, 50, 'Electronics', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Wireless Mouse', 'Ergonomic wireless mouse with precision tracking', 'MOU-001', 29.99, 200, 'Electronics', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Mechanical Keyboard', 'RGB backlit mechanical keyboard with Cherry MX switches', 'KEY-001', 149.99, 75, 'Electronics', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('USB-C Hub', '7-in-1 USB-C hub with HDMI, USB 3.0, SD card reader', 'HUB-001', 49.99, 150, 'Electronics', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Webcam HD', '1080p HD webcam with built-in microphone', 'CAM-001', 79.99, 100, 'Electronics', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Furniture
    ('Office Chair', 'Adjustable ergonomic office chair with lumbar support', 'CHR-001', 249.99, 30, 'Furniture', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Standing Desk', 'Electric height-adjustable standing desk 60x30 inches', 'DSK-001', 599.99, 15, 'Furniture', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Monitor Stand', 'Bamboo monitor stand with storage drawer', 'STD-001', 39.99, 80, 'Furniture', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Stationery
    ('Notebook Set', 'Premium notebook set with 3 notebooks and pen', 'NOT-001', 19.99, 500, 'Stationery', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Desk Organizer', 'Bamboo desk organizer with multiple compartments', 'ORG-001', 24.99, 120, 'Stationery', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Sticky Notes', 'Colorful sticky notes pack (5 colors, 500 sheets)', 'STK-001', 9.99, 300, 'Stationery', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Low Stock Items (for testing low-stock endpoint)
    ('Limited Edition Pen', 'Collector edition fountain pen', 'PEN-001', 199.99, 3, 'Stationery', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Rare Book', 'Vintage programming book', 'BOK-001', 49.99, 2, 'Books', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Vintage Typewriter', 'Restored vintage typewriter', 'TYP-001', 899.99, 1, 'Electronics', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- Inactive Product (for testing)
    ('Discontinued Item', 'This product is no longer available', 'DIS-001', 99.99, 0, 'Electronics', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (sku) DO NOTHING;

-- ============================================================================
-- ORDERS
-- ============================================================================
-- Create sample orders for testing order queries
INSERT INTO orders (user_id, order_number, status, total_amount, shipping_address, created_at, updated_at)
SELECT 
    u.id,
    'ORD-DEMO-001',
    'PENDING',
    1299.99,
    '123 Main St, City, State 12345',
    CURRENT_TIMESTAMP - INTERVAL '2 days',
    CURRENT_TIMESTAMP - INTERVAL '2 days'
FROM users u
WHERE u.email = 'alice@example.com'
ON CONFLICT (order_number) DO NOTHING;

INSERT INTO orders (user_id, order_number, status, total_amount, shipping_address, created_at, updated_at)
SELECT 
    u.id,
    'ORD-DEMO-002',
    'PROCESSING',
    179.98,
    '456 Oak Ave, City, State 67890',
    CURRENT_TIMESTAMP - INTERVAL '1 day',
    CURRENT_TIMESTAMP - INTERVAL '12 hours'
FROM users u
WHERE u.email = 'bob@example.com'
ON CONFLICT (order_number) DO NOTHING;

INSERT INTO orders (user_id, order_number, status, total_amount, shipping_address, created_at, updated_at)
SELECT 
    u.id,
    'ORD-DEMO-003',
    'SHIPPED',
    49.99,
    '789 Pine Rd, City, State 11111',
    CURRENT_TIMESTAMP - INTERVAL '3 days',
    CURRENT_TIMESTAMP - INTERVAL '1 day'
FROM users u
WHERE u.email = 'charlie@example.com'
ON CONFLICT (order_number) DO NOTHING;

INSERT INTO orders (user_id, order_number, status, total_amount, shipping_address, created_at, updated_at)
SELECT 
    u.id,
    'ORD-DEMO-004',
    'DELIVERED',
    249.99,
    '321 Elm St, City, State 22222',
    CURRENT_TIMESTAMP - INTERVAL '5 days',
    CURRENT_TIMESTAMP - INTERVAL '2 days'
FROM users u
WHERE u.email = 'diana@example.com'
ON CONFLICT (order_number) DO NOTHING;

INSERT INTO orders (user_id, order_number, status, total_amount, shipping_address, created_at, updated_at)
SELECT 
    u.id,
    'ORD-DEMO-005',
    'CANCELLED',
    599.99,
    '654 Maple Dr, City, State 33333',
    CURRENT_TIMESTAMP - INTERVAL '1 day',
    CURRENT_TIMESTAMP - INTERVAL '1 day'
FROM users u
WHERE u.email = 'alice@example.com'
ON CONFLICT (order_number) DO NOTHING;

-- ============================================================================
-- ORDER ITEMS (for orders with items)
-- ============================================================================
-- Add items to ORD-DEMO-002 (Processing order)
INSERT INTO order_items (order_id, product_id, quantity, price, created_at)
SELECT 
    o.id,
    p.id,
    2,
    29.99,
    o.created_at
FROM orders o
CROSS JOIN products p
WHERE o.order_number = 'ORD-DEMO-002'
  AND p.sku = 'MOU-001'
  AND NOT EXISTS (
    SELECT 1 FROM order_items oi 
    WHERE oi.order_id = o.id AND oi.product_id = p.id
  );

INSERT INTO order_items (order_id, product_id, quantity, price, created_at)
SELECT 
    o.id,
    p.id,
    1,
    49.99,
    o.created_at
FROM orders o
CROSS JOIN products p
WHERE o.order_number = 'ORD-DEMO-002'
  AND p.sku = 'HUB-001'
  AND NOT EXISTS (
    SELECT 1 FROM order_items oi 
    WHERE oi.order_id = o.id AND oi.product_id = p.id
  );

-- Add items to ORD-DEMO-003 (Shipped order)
INSERT INTO order_items (order_id, product_id, quantity, price, created_at)
SELECT 
    o.id,
    p.id,
    1,
    49.99,
    o.created_at
FROM orders o
CROSS JOIN products p
WHERE o.order_number = 'ORD-DEMO-003'
  AND p.sku = 'HUB-001'
  AND NOT EXISTS (
    SELECT 1 FROM order_items oi 
    WHERE oi.order_id = o.id AND oi.product_id = p.id
  );

-- Add items to ORD-DEMO-004 (Delivered order)
INSERT INTO order_items (order_id, product_id, quantity, price, created_at)
SELECT 
    o.id,
    p.id,
    1,
    249.99,
    o.created_at
FROM orders o
CROSS JOIN products p
WHERE o.order_number = 'ORD-DEMO-004'
  AND p.sku = 'CHR-001'
  AND NOT EXISTS (
    SELECT 1 FROM order_items oi 
    WHERE oi.order_id = o.id AND oi.product_id = p.id
  );

-- ============================================================================
-- SUMMARY
-- ============================================================================
-- This seed data provides:
-- - 6 users (5 active, 1 inactive)
-- - 3 API keys (PREMIUM, BASIC, STANDARD tiers)
-- - 15 products (various categories, stock levels, including low-stock items)
-- - 5 sample orders (different statuses: PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED)
-- - Order items for some orders
--
-- Use this data to test:
-- - User management endpoints
-- - Product catalog with filtering and search
-- - Order lifecycle and status updates
-- - Low stock product queries
-- - Order history by user
-- - Rate limiting with different tiers
