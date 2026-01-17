# Rate Limit Test - Troubleshooting

## Vấn Đề: Rate Limiting Không Hoạt Động

### Nguyên Nhân

**Test đang chạy với:**
- API Key: `test-api-key-local-dev`
- TIER parameter: `BASIC` (60 req/min)
- **Nhưng:** API key này có tier **PREMIUM** (1000 req/min)!

**Kết quả:**
- Test gửi 70 requests
- PREMIUM tier cho phép 1000 req/min
- 70 < 1000 → Không bị rate limit
- Tất cả requests đều 200 OK

### Giải Pháp

**Option 1: Dùng Đúng Tier (Recommended)**

API key `test-api-key-local-dev` có tier PREMIUM, nên test với PREMIUM:

```bash
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env API_KEY=test-api-key-local-dev \
  --env TIER=PREMIUM \
  rate-limit-test.js
```

Test sẽ gửi 1010 requests (1000 + 10), requests 1001-1010 sẽ bị rate limit.

**Option 2: Tạo API Key với Tier BASIC**

Cần tạo API key mới với tier BASIC trong database:

```sql
-- Tạo API key với tier BASIC
INSERT INTO api_keys (key_hash, user_id, name, scopes, rate_limit_tier, is_active, created_at, updated_at)
SELECT 
    'basic-key-hash-' || u.id,
    u.id,
    'Basic Tier Key for Testing',
    ARRAY['read']::TEXT[],
    'BASIC',  -- Tier BASIC
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'test@example.com';
```

Sau đó dùng API key đó trong test.

**Option 3: Test với PREMIUM Tier**

Nếu muốn test PREMIUM tier (1000 req/min):

```bash
# Test PREMIUM tier
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env API_KEY=test-api-key-local-dev \
  --env TIER=PREMIUM \
  rate-limit-test.js
```

Test sẽ gửi 1010 requests, requests 1001-1010 sẽ bị rate limit.

## Kiểm Tra API Key Tier

```bash
# Kiểm tra API key tier trong database
psql -h localhost -U postgres -d apidb_dev -c "
SELECT 
    ak.name,
    ak.rate_limit_tier,
    u.email
FROM api_keys ak
JOIN users u ON ak.user_id = u.id
WHERE u.email = 'test@example.com';
"
```

## Logic Test

**Expected Behavior:**
- Requests 1-{LIMIT}: Should return 200 OK ✅
- Requests {LIMIT+1}-{TEST_REQUESTS}: Should return 429 Too Many Requests ✅

**Current Issue:**
- Tất cả requests đều 200 vì API key có tier cao hơn expected
- Script logic đã được sửa để log đúng

## Sửa Script

Script đã được cập nhật:
1. ✅ Logic đúng: 200 OK là success, không phải error
2. ✅ Thêm delay nhỏ giữa requests
3. ✅ Cải thiện logging

## Chạy Lại Test

```bash
cd load-tests/k6

# Test với PREMIUM tier (match với API key)
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env API_KEY=test-api-key-local-dev \
  --env TIER=PREMIUM \
  rate-limit-test.js
```

**Expected Results:**
- Successful Requests: ~1000 (requests 1-1000)
- Rate Limited Requests: ~10 (requests 1001-1010)
- Accuracy: >99%

---

**Lưu ý:** Đảm bảo API key tier khớp với TIER parameter trong test!

