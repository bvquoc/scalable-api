# k6 Load Tests

Modern k6 load tests for spike testing and rate limit validation.

## Prerequisites

### Installation

**macOS (Homebrew):**
```bash
brew install k6
```

**Linux (Debian/Ubuntu):**
```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

**Docker:**
```bash
docker pull grafana/k6:latest
```

**Verify Installation:**
```bash
k6 version
# Expected: k6 v0.48.0 or higher
```

## Test Scripts

### 1. Spike Test (`spike-test.js`)

**Scenario:** Sudden traffic spike to validate system resilience

**Load Pattern:**
- 1 min: Ramp to 100 users (baseline)
- 30 sec: Sudden spike to 500 users
- 1 min: Recovery to 100 users
- 30 sec: Ramp down to 0

**Total Duration:** 3 minutes

**Performance Targets:**
- p95 response time: <500ms during spike
- Error rate: <1% during spike
- Recovery time: <30s after spike
- Cache hit rate: >90% (products endpoint)

### 2. Rate Limit Test (`rate-limit-test.js`)

**Scenario:** Validate distributed rate limiting accuracy

**Test:** Single user, rapid-fire requests (no think time)

**Rate Limit Tiers:**
- BASIC: 60 requests/minute
- STANDARD: 300 requests/minute
- PREMIUM: 1000 requests/minute

**Success Criteria:**
- Requests 1-N pass (200 OK) where N = tier limit
- Request N+1 fails (429 Too Many Requests)
- Retry-After header present
- Enforcement accuracy: >99%

## Running Tests

### Spike Test

```bash
# Navigate to k6 directory
cd load-tests/k6

# Run spike test with defaults
k6 run spike-test.js

# Run with custom parameters
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env API_KEY=test-api-key-local-dev \
  spike-test.js

# Run with Docker
docker run --rm \
  -v $(pwd):/scripts \
  --network="host" \
  grafana/k6:latest run \
  --env BASE_URL=http://localhost:8080 \
  --env API_KEY=test-api-key-local-dev \
  /scripts/spike-test.js
```

### Rate Limit Test

```bash
# Test BASIC tier (60 req/min)
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env API_KEY=test-api-key-local-dev \
  --env TIER=BASIC \
  rate-limit-test.js

# Test STANDARD tier (300 req/min)
k6 run \
  --env TIER=STANDARD \
  rate-limit-test.js

# Test PREMIUM tier (1000 req/min)
k6 run \
  --env TIER=PREMIUM \
  rate-limit-test.js
```

## Test Execution Steps

### Spike Test Execution

**1. Pre-Test Checklist:**
```bash
# Verify API health
curl -H "X-API-Key: test-api-key-local-dev" http://localhost:8080/actuator/health

# Clear Redis cache
docker exec scalable-api-redis redis-cli FLUSHDB

# Monitor in separate terminal
watch -n 1 'curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq ".measurements[0].value"'
```

**2. Execute Test:**
```bash
cd load-tests/k6

# Run spike test (3 minutes)
k6 run \
  --out json=../results/k6-spike/spike-metrics.json \
  spike-test.js

# Expected output:
#
#          /\      |‾‾| /‾‾/   /‾‾/
#     /\  /  \     |  |/  /   /  /
#    /  \/    \    |     (   /   ‾‾\
#   /          \   |  |\  \ |  (‾)  |
#  / __________ \  |__| \__\ \_____/ .io
#
#   execution: local
#      script: spike-test.js
#      output: json (../results/k6-spike/spike-metrics.json)
#
#   scenarios: (100.00%) 1 scenario, 500 max VUs, 3m30s max duration
#
#   ✓ GET /api/products: status is 200
#   ✓ GET /api/users: status is 200
#   ✓ POST /api/events: status is 202
#
#   checks.........................: 99.85% ✓ 29955      ✗ 45
#   data_received..................: 45 MB  250 kB/s
#   data_sent......................: 12 MB  67 kB/s
#   http_req_duration..............: avg=85ms   min=10ms med=50ms max=1.2s p(90)=150ms p(95)=280ms
#   http_req_failed................: 0.15%  ✓ 45        ✗ 29955
#   http_reqs......................: 30000  166.67/s
#   iteration_duration.............: avg=3.5s   min=3s   med=3.4s max=4.2s p(90)=3.8s  p(95)=4s
#   iterations.....................: 10000  55.56/s
#   vus............................: 100    min=0        max=500
#   vus_max........................: 500    min=500      max=500
```

**3. Analyze Results:**
```bash
# View results summary
cat ../results/k6-spike/spike-test-summary.txt

# Extract peak spike metrics (30s spike window)
jq '.metrics.http_req_duration.values' ../results/k6-spike/spike-metrics.json
```

### Rate Limit Test Execution

**1. Execute BASIC Tier Test:**
```bash
cd load-tests/k6

# Test BASIC tier (60 req/min limit)
k6 run \
  --env TIER=BASIC \
  --env API_KEY=test-api-key-local-dev \
  rate-limit-test.js

# Expected output:
# ============================================================
# k6 Rate Limit Validation Test - BASIC Tier
# ============================================================
#
# Rate Limit: 60 requests/minute
# Total Requests: 70
#
# Results:
#   Successful Requests: 60 (expected: 60)
#   Rate Limited Requests: 10 (expected: 10)
#   Unexpected Errors: 0
#
# Accuracy: 100.00%
# Status: ✓ PASS
#
# ✓ Rate limiting enforced correctly for BASIC tier
# ============================================================
```

**2. Verify Rate Limit Headers:**
```bash
# Manual verification with curl
for i in {1..70}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
    -H "X-API-Key: test-api-key-local-dev" \
    http://localhost:8080/api/users
  sleep 0.1
done

# Expected: 60x "200", 10x "429"
```

**3. Analyze Results:**
```bash
# View detailed results
cat ../results/k6-rate-limit/rate-limit-test-summary.txt
cat ../results/k6-rate-limit/rate-limit-test-results.json | jq '.'
```

## Understanding Results

### k6 Metrics

**Standard Metrics:**
- `http_req_duration`: Total request duration (send + wait + receive)
- `http_req_waiting`: Time to first byte (TTFB)
- `http_req_connecting`: Connection establishment time
- `http_req_sending`: Time sending request
- `http_req_receiving`: Time receiving response
- `http_req_failed`: Percentage of failed requests
- `http_reqs`: Total number of HTTP requests
- `iterations`: Number of VU iterations completed
- `vus`: Current active virtual users
- `checks`: Percentage of successful checks

**Custom Metrics (Spike Test):**
- `errors`: Custom error rate counter
- `product_latency`: Response time trend for products endpoint
- `user_latency`: Response time trend for users endpoint

### Thresholds

k6 automatically evaluates thresholds during test execution:

```javascript
✓ http_req_duration p(95) < 500ms  // Pass
✗ http_req_failed rate < 0.01      // Fail (1.5% error rate)
✓ errors rate < 0.01               // Pass
✓ product_latency p(95) < 200ms    // Pass (cached)
```

**Exit Code:**
- 0: All thresholds passed
- 99: At least one threshold failed

## Troubleshooting

### Common Issues

**1. k6 Not Found**
```bash
# Error: command not found: k6
# Solution: Install k6 or use Docker
docker run --rm grafana/k6:latest version
```

**2. API Connection Refused**
```bash
# Error: dial tcp 127.0.0.1:8080: connect: connection refused
# Solution: Verify API is running
curl http://localhost:8080/actuator/health
```

**3. Rate Limit Not Enforced**
```bash
# Issue: All requests return 200 OK, no 429
# Solution: Check API key tier in database
docker exec scalable-api-postgres psql -U postgres -d apidb_dev \
  -c "SELECT name, rate_limit_tier, is_active FROM api_keys;"

# Verify Redis is running
docker exec scalable-api-redis redis-cli PING
# Expected: PONG
```

**4. Spike Causes API Crash**
```bash
# Issue: API becomes unresponsive during spike
# Solution: Check container resources
docker stats scalable-api

# Increase Docker resource limits (Docker Desktop → Settings → Resources)
# Recommended: 4 CPU, 8GB RAM
```

## Performance Analysis

### Spike Test Analysis

**Metrics to Evaluate:**
1. **Response Time Degradation:**
   - Baseline (100 users): p95 = 100ms
   - Spike (500 users): p95 = 400ms
   - Degradation: 4x increase (acceptable if <500ms)

2. **Error Rate:**
   - Spike period: 0.5% error rate (acceptable)
   - Recovery period: 0.1% error rate (good)

3. **Recovery Time:**
   - Time from spike end to baseline performance: <30s (target met)

4. **Throughput:**
   - Peak throughput: 250 req/sec
   - Sustained throughput: 150 req/sec

### Rate Limit Analysis

**Accuracy Calculation:**
```
Accuracy = (Actual Successful / Expected Successful) × 100%
         = (60 / 60) × 100% = 100%

Rate Limit Accuracy = (Actual Rate Limited / Expected Rate Limited) × 100%
                    = (10 / 10) × 100% = 100%
```

**Pass Criteria:** Accuracy ≥ 99%

## Best Practices

1. **Use environment variables** for configuration (never hardcode)
2. **Run tests multiple times** for statistical significance
3. **Monitor API server** during tests (Grafana dashboard)
4. **Compare results** with JMeter/Gatling for consistency
5. **Document anomalies** (errors, timeouts, crashes)
6. **Clean state** between tests (flush Redis cache)

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Load Tests
on: [push]

jobs:
  k6-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run k6 spike test
        uses: grafana/k6-action@v0.3.1
        with:
          filename: load-tests/k6/spike-test.js
          flags: --out cloud
        env:
          K6_CLOUD_TOKEN: ${{ secrets.K6_CLOUD_TOKEN }}
```

## Next Steps

1. Execute spike test and collect metrics
2. Execute rate limit validation for all tiers
3. Compare with JMeter baseline results
4. Document findings in performance reports
5. Create final research paper

## References

- [k6 Documentation](https://k6.io/docs/)
- [k6 HTTP Module](https://k6.io/docs/javascript-api/k6-http/)
- [k6 Metrics](https://k6.io/docs/using-k6/metrics/)
- [k6 Thresholds](https://k6.io/docs/using-k6/thresholds/)
