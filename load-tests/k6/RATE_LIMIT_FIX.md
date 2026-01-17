# Rate Limit Test - Fix Guide

## Vấn Đề Phát Hiện

### 1. Logic Script Bị Ngược ✅ Đã Sửa

**Vấn đề:**
- Script log "ERROR: Unexpected status 200" cho requests 1-60
- Nhưng 200 OK là **expected** cho requests trước limit!

**Đã sửa:**
- Logic đã được cập nhật để xử lý đúng
- 200 OK = success (không phải error)

### 2. Rate Limiting Không Hoạt Động ⚠️ Cần Kiểm Tra

**Vấn đề:**
- Test dùng `TIER=BASIC` (expect 60 req/min)
- API key `test-api-key-local-dev` có tier **PREMIUM** (1000 req/min)
- Test gửi 70 requests → Không vượt limit PREMIUM → Tất cả 200 OK

**Giải pháp:**

#### Option A: Test với PREMIUM Tier (Recommended)

```bash
cd load-tests/k6

# Test với PREMIUM tier (match với API key)
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env API_KEY=test-api-key-local-dev \
  --env TIER=PREMIUM \
  rate-limit-test.js
```

Test sẽ gửi 1010 requests (1000 + 10):
- Requests 1-1000: 200 OK ✅
- Requests 1001-1010: 429 Too Many Requests ✅

#### Option B: Tạo API Key với Tier BASIC

Cần tạo API key mới với tier BASIC:

```sql
-- Connect to database
psql -h localhost -U postgres -d apidb_dev

-- Tạo API key với tier BASIC
INSERT INTO api_keys (key_hash, user_id, name, scopes, rate_limit_tier, is_active, created_at, updated_at)
SELECT 
    'basic-test-key-hash-' || u.id,
    u.id,
    'Basic Tier Test Key',
    ARRAY['read']::TEXT[],
    'BASIC',  -- Tier BASIC (60 req/min)
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'test@example.com'
ON CONFLICT (key_hash) DO NOTHING;
```

Sau đó hash API key và dùng trong test.

## Kiểm Tra API Key Tier

```bash
# Kiểm tra tier của API key
psql -h localhost -U postgres -d apidb_dev -c "
SELECT 
    ak.name,
    ak.rate_limit_tier,
    u.email,
    LEFT(ak.key_hash, 20) as key_hash_prefix
FROM api_keys ak
JOIN users u ON ak.user_id = u.id
WHERE u.email = 'test@example.com';
"
```

**Expected output:**
```
name                          | rate_limit_tier | email              | key_hash_prefix
------------------------------+-----------------+--------------------+------------------
Local Development Key (PREMIUM) | PREMIUM        | test@example.com   | 489545ff5724037835
```

## Chạy Test Đúng Cách

### Test PREMIUM Tier (1000 req/min)

```bash
cd load-tests/k6

k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env API_KEY=test-api-key-local-dev \
  --env TIER=PREMIUM \
  rate-limit-test.js
```

**Expected Results:**
- Total Requests: 1010
- Successful Requests: ~1000 (requests 1-1000)
- Rate Limited Requests: ~10 (requests 1001-1010)
- Accuracy: >99%

### Test BASIC Tier (60 req/min)

Cần API key với tier BASIC. Xem Option B ở trên.

## Debug Rate Limiting

### Kiểm Tra Rate Limit Key trong Redis

```bash
# Connect to Redis
redis-cli

# List all rate limit keys
KEYS ratelimit:*

# Check specific key
GET ratelimit:489545ff5724037835fceb90b6533abcd4b7c23c25e58fddc6f433e43278b078:2026011710

# TTL của key
TTL ratelimit:489545ff5724037835fceb90b6533abcd4b7c23c25e58fddc6f433e43278b078:2026011710
```

### Kiểm Tra Logs

```bash
# Xem application logs
tail -f logs/application.log | grep -i "rate limit"
```

## Lưu Ý Quan Trọng

1. **API Key Tier phải match với TIER parameter:**
   - `test-api-key-local-dev` → PREMIUM → Use `TIER=PREMIUM`
   - BASIC tier key → Use `TIER=BASIC`

2. **Rate limiting dùng 1-minute windows:**
   - Tất cả requests phải trong cùng 1 phút để hit limit
   - Script không có sleep để đảm bảo rapid fire

3. **Test với PREMIUM tier:**
   - Gửi 1010 requests (1000 + 10)
   - Requests 1001-1010 sẽ bị rate limit

---

**Quick Fix:** Chạy test với `TIER=PREMIUM` thay vì `TIER=BASIC`!

