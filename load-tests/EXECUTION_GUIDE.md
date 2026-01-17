# Phase 9: Complete Execution Guide

**Step-by-step guide to execute all load tests and generate reports**

## Pre-Flight Checklist

- [ ] All dependencies installed (JMeter, Gatling, k6)
- [ ] Docker services running (PostgreSQL, Redis, RabbitMQ, Kafka)
- [ ] Spring Boot application running
- [ ] Monitoring stack running (Prometheus + Grafana)
- [ ] Test API key available (`test-api-key-local-dev`)

## Test Execution Sequence

### Day 1: Baseline & Setup (2-3 hours)

#### 1. Baseline Performance Test (10 minutes)

**Purpose:** Establish baseline metrics under normal load

```bash
cd load-tests/jmeter

# Start metrics collection
../scripts/collect-metrics.sh baseline-test 600 10 &

# Run baseline test
jmeter -n -t baseline-test.jmx \
  -JbaseUrl=http://localhost:8080 \
  -JapiKey=test-api-key-local-dev \
  -Jthreads=100 \
  -Jrampup=60 \
  -Jduration=600 \
  -l ../results/jmeter-baseline/baseline-results.jtl \
  -e -o ../results/jmeter-baseline/report/

# View results
open ../results/jmeter-baseline/report/index.html
```

**Expected Results:**
- p95 response time: <200ms
- Throughput: 800-1,200 req/sec
- Error rate: <0.5%

**Metrics to Record:**
- Response times (p50, p95, p99)
- Throughput (requests/sec)
- Error rate (%)
- Cache hit rate (from Prometheus)

---

#### 2. Rate Limiting Validation (5 minutes)

**Purpose:** Verify rate limiting enforcement accuracy

```bash
cd load-tests/k6

# Test BASIC tier (60 req/min)
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env API_KEY=test-api-key-local-dev \
  --env TIER=BASIC \
  rate-limit-test.js

# Test PREMIUM tier (1000 req/min)
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env API_KEY=test-api-key-local-dev \
  --env TIER=PREMIUM \
  rate-limit-test.js
```

**Expected Results:**
- BASIC: ~60 requests succeed, ~10 get 429
- PREMIUM: ~1000 requests succeed, ~100 get 429
- Accuracy: >99%

---

### Day 2: Stress & Spike Tests (2-3 hours)

#### 3. Stress Test (15 minutes)

**Purpose:** Find breaking point by gradually increasing load

```bash
cd load-tests/gatling

# Run stress test
mvn gatling:test \
  -Dgatling.simulationClass=simulations.StressTestSimulation \
  -DbaseUrl=http://localhost:8080 \
  -DapiKey=test-api-key-local-dev

# View results
open target/gatling/stresstestsimulation-*/index.html
```

**Analysis Points:**
- At what user count does p95 exceed 500ms?
- When does error rate exceed 5%?
- What resource exhausts first?

---

#### 4. Spike Test (3 minutes)

**Purpose:** Validate system resilience to sudden traffic spikes

```bash
cd load-tests/k6

# Run spike test
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env API_KEY=test-api-key-local-dev \
  spike-test.js

# View results
cat ../results/k6-spike/spike-test-summary.json | jq
```

**Expected Behavior:**
- System handles spike without crashing
- Response times degrade gracefully
- Error rate stays <5%
- Recovery time <30s

---

### Day 3: Comparative Benchmarks (2-3 hours)

#### 5. Cache Comparison Test (15 minutes)

**Purpose:** Quantify Redis caching impact

```bash
cd load-tests
./scripts/compare-cache.sh
```

**Expected Comparison:**
- Cache Enabled: p95 <100ms, 3-5x throughput
- Cache Disabled: p95 200-300ms, lower throughput
- DB Query Reduction: 99%+ with caching

---

#### 6. Endurance Test (1 hour - Optional)

**Purpose:** Detect memory leaks and connection pool exhaustion

```bash
cd load-tests/jmeter

# Start monitoring
watch -n 5 'curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq' &
jstat -gcutil $(pgrep -f "spring-boot") 5000 &

# Run endurance test
jmeter -n -t endurance-test.jmx \
  -JbaseUrl=http://localhost:8080 \
  -JapiKey=test-api-key-local-dev \
  -Jthreads=500 \
  -Jduration=3600 \
  -l ../results/jmeter-endurance/endurance-results.jtl \
  -e -o ../results/jmeter-endurance/report/
```

**Red Flags:**
- Heap usage continuously grows
- Old Gen GC frequency increases
- Connection pool exhaustion
- Response time degradation over time

---

## Results Collection

### Automated Collection

```bash
# Collect metrics during test
cd load-tests
./scripts/collect-metrics.sh <test-name> <duration-seconds> [interval-seconds]

# Example: Collect metrics every 10 seconds for 10 minutes
./scripts/collect-metrics.sh baseline-test 600 10
```

### Manual Collection

**Prometheus Metrics:**
```bash
curl http://localhost:8080/actuator/prometheus > results/prometheus-metrics.txt
```

**Grafana Dashboard:**
- Access: http://localhost:3000
- Export dashboard JSON

**JVM Thread Dump:**
```bash
jstack $(pgrep -f "spring-boot") > results/thread-dump.txt
```

**Heap Dump (if needed):**
```bash
jmap -dump:live,format=b,file=results/heap-dump.hprof $(pgrep -f "spring-boot")
```

---

## Report Generation

### Generate Individual Reports

```bash
cd load-tests

# Baseline report
./scripts/generate-report.sh baseline

# Stress test report
./scripts/generate-report.sh stress

# Spike test report
./scripts/generate-report.sh spike

# Endurance test report
./scripts/generate-report.sh endurance

# Rate limit validation report
./scripts/generate-report.sh rate-limit

# Comparative analysis report
./scripts/generate-report.sh comparative
```

### Update Reports with Actual Metrics

1. Open generated report: `plans/reports/performance-*-report.md`
2. Extract metrics from test results:
   - JMeter: View HTML report → Copy metrics
   - Gatling: View HTML report → Copy metrics
   - k6: Parse JSON summary → Extract metrics
3. Update report tables with actual values
4. Add analysis and recommendations

---

## Results Analysis

### JMeter Results

**View HTML Report:**
```bash
open load-tests/results/jmeter-baseline/report/index.html
```

**Key Metrics:**
- Response Times (p50, p95, p99)
- Throughput (requests/sec)
- Error Rate (%)
- Response Codes Distribution

**Extract Metrics:**
```bash
# Parse JTL file (if needed)
grep -E "GET /api/users|GET /api/products" \
  load-tests/results/jmeter-baseline/baseline-results.jtl | \
  awk -F',' '{print $2, $3}' | sort -n
```

### Gatling Results

**View HTML Report:**
```bash
open load-tests/gatling/target/gatling/stresstestsimulation-*/index.html
```

**Key Metrics:**
- Response Time Distribution
- Active Users Over Time
- Request Rate
- Success Rate

### k6 Results

**View JSON Summary:**
```bash
cat load-tests/results/k6-spike/spike-test-summary.json | jq
```

**Key Metrics:**
- `http_req_duration` (p50, p95, p99)
- `http_reqs` (total, rate)
- `http_req_failed` (error rate)

---

## Performance Targets Validation

### Check Against Targets

| Metric | Target | How to Verify |
|--------|--------|---------------|
| Response Time (p95) | <200ms | JMeter/Gatling HTML report |
| Throughput | >1,000 req/s | JMeter Summary Report |
| Cache Hit Rate | >90% | Prometheus metrics |
| Error Rate | <1% | Test summary reports |
| Rate Limit Accuracy | >99% | k6 rate-limit-test results |

---

## Troubleshooting

### Issue: Test fails immediately

**Check:**
1. Application is running: `curl http://localhost:8080/actuator/health`
2. API key is valid: `curl -H "X-API-Key: test-api-key-local-dev" http://localhost:8080/api/users`
3. Services are running: `docker-compose ps`

### Issue: High error rate

**Possible causes:**
1. Rate limiting (expected for rate-limit tests)
2. Application overloaded (reduce load)
3. Database connection pool exhausted (check HikariCP metrics)
4. Redis unavailable (check Redis status)

### Issue: Low throughput

**Possible causes:**
1. Network bottleneck
2. Database slow queries (check query logs)
3. Cache miss rate high (check Redis metrics)
4. Application CPU/memory constrained

---

## Next Steps After Testing

1. **Review all reports** in `plans/reports/`
2. **Compare metrics** with targets
3. **Identify bottlenecks** and optimization opportunities
4. **Update configuration** based on findings
5. **Re-run tests** to validate improvements
6. **Document findings** in research paper

---

## Time Estimates

| Test | Duration | Analysis Time | Total |
|------|----------|---------------|-------|
| Baseline | 10 min | 30 min | 40 min |
| Rate Limit | 1 min | 15 min | 16 min |
| Stress | 15 min | 45 min | 60 min |
| Spike | 3 min | 20 min | 23 min |
| Cache Comparison | 15 min | 30 min | 45 min |
| Endurance | 60 min | 30 min | 90 min |
| **Total** | **~104 min** | **~3 hours** | **~5 hours** |

---

**Status:** Ready for execution ✅

