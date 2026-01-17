# Gatling Test Fix Notes

## Vấn Đề Đã Sửa

### 1. Duplicate Scenario Names ✅

**Lỗi:**
```
Scenario names must be unique but found duplicates: List(Read-Heavy Workload)
```

**Nguyên nhân:**
- Có nhiều `readHeavyScenario.inject()` với cùng tên scenario
- Gatling yêu cầu mỗi scenario trong `setUp()` phải có tên unique

**Đã sửa:**
- Thay đổi từ nhiều `inject()` riêng biệt → một `inject()` duy nhất với `rampUsersPerSec()`
- Sử dụng pattern: `rampUsersPerSec(from).to(to).during(duration)`
- Kết hợp tất cả phases vào một scenario

**Before:**
```scala
readHeavyScenario.inject(rampUsers(100).during(1.minute)),
readHeavyScenario.inject(rampUsers(200).during(1.minute)),
// ... nhiều scenarios với cùng tên
```

**After:**
```scala
readHeavyScenario.inject(
  rampUsersPerSec(0).to(100).during(1.minute),
  rampUsersPerSec(100).to(200).during(1.minute),
  // ... tất cả phases trong một inject()
)
```

### 2. Deprecated Gatling EL Syntax ✅

**Warning:**
```
You're still using the deprecated ${} pattern for Gatling EL. 
Please use the #{} pattern instead.
```

**Đã sửa:**
- Thay tất cả `${variable}` → `#{variable}`
- Thay `${__Random(...)}` → `#{__Random(...)}`

**Examples:**
- `${userPage}` → `#{userPage}`
- `${productPage}` → `#{productPage}`
- `${orderId}` → `#{orderId}`
- `${__Random(1,1000)}` → `#{__Random(1,1000)}`

## Load Profile Mới

**Pattern:** Gradual ramp với `rampUsersPerSec()`

```
Phase 1: 0 → 100 users/sec (1 min)
Phase 2: 100 → 200 users/sec (1 min)
Phase 3: 200 → 400 users/sec (1 min)
Phase 4: 400 → 600 users/sec (1 min)
Phase 5: 600 → 800 users/sec (1 min)
Phase 6: 800 → 1000 users/sec (1 min)
Hold: 1000 users/sec (1 min)
```

**Total Duration:** ~7 minutes

## Chạy Test

```bash
cd load-tests/gatling

# Compile
mvn clean test-compile

# Run test
mvn gatling:test \
  -DbaseUrl=http://localhost:8080 \
  -DapiKey=test-api-key-local-dev
```

## Kết Quả

Sau khi sửa:
- ✅ Compile thành công
- ✅ Không còn duplicate scenario names
- ✅ Không còn deprecated syntax warnings
- ✅ Load profile hoạt động đúng

---

**Status:** Fixed and ready to run ✅

