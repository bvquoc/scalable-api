# Phase 9: Load Testing & Performance Analysis

**Comprehensive performance benchmarking and load testing guide**

## 📋 Overview

This directory contains all load testing scripts, configurations, and results for Phase 9. The goal is to:

- Establish baseline performance metrics
- Identify system breaking points
- Validate rate limiting effectiveness
- Compare Redis caching impact
- Analyze horizontal scaling behavior

## 🛠️ Prerequisites

### 1. Install Load Testing Tools

**JMeter 5.6+ (Baseline & Endurance Tests):**

```bash
# macOS
brew install jmeter

# Linux
wget https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.6.3.tgz
tar -xzf apache-jmeter-5.6.3.tgz
export PATH=$PATH:$(pwd)/apache-jmeter-5.6.3/bin

# Verify
jmeter --version
```

**Gatling 3.10+ (Stress Tests):**

```bash
# macOS
brew install gatling

# Or download from https://gatling.io/open-source/
# Verify
gatling.sh --version
```

**k6 (Spike & Rate Limit Tests):**

```bash
# macOS
brew install k6

# Linux
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 \
  --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | \
  sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6

# Verify
k6 version
```

### 2. Start Application & Dependencies

```bash
# Start all services (PostgreSQL, Redis, RabbitMQ, Kafka)
cd /Users/quocbui/src/uit/DA2/scalable-api
docker-compose up -d

# Start monitoring stack (Prometheus + Grafana)
docker-compose -f docker-compose-monitoring.yml up -d

# Start Spring Boot application
mvn spring-boot:run

# Verify application is running
curl http://localhost:8080/actuator/health
```

### 3. Verify Test API Key

The test API key should be available:

```bash
# Test API key: test-api-key-local-dev
curl -H "X-API-Key: test-api-key-local-dev" \
     http://localhost:8080/api/users
```

## 📊 Test Scenarios

### Scenario 1: Baseline Performance Test (JMeter)

**Goal:** Establish baseline metrics under normal load

**Configuration:**

- Users: 100 concurrent
- Duration: 10 minutes
- Ramp-up: 60 seconds
- Endpoints: Mixed (40% GET users, 40% GET products, 20% GET orders)

**Run Test:**

```bash
cd load-tests/jmeter

# Run baseline test
jmeter -n -t baseline-test.jmx \
  -JbaseUrl=http://localhost:8080 \
  -JapiKey=test-api-key-local-dev \
  -Jthreads=100 \
  -Jrampup=60 \
  -Jduration=600 \
  -l ../results/jmeter-baseline/baseline-results.jtl \
  -e -o ../results/jmeter-baseline/report/

# View HTML report
open ../results/jmeter-baseline/report/index.html
```

**Expected Results:**

- p50 response time: <100ms
- p95 response time: <200ms
- p99 response time: <500ms
- Throughput: 800-1,200 requests/sec
- Error rate: <0.5%

---

### Scenario 2: Stress Test (Gatling)

**Goal:** Find breaking point by gradually increasing load

**Configuration:**

- Users: 100 → 1000 (gradual ramp)
- Duration: ~15 minutes total
- Pattern: 100 → 200 → 400 → 600 → 800 → 1000 users

**Run Test:**

```bash
cd load-tests/gatling

# Run stress test
mvn gatling:test -Dgatling.simulationClass=simulations.StressTestSimulation \
  -DbaseUrl=http://localhost:8080 \
  -DapiKey=test-api-key-local-dev

# Results are in: target/gatling/
# View HTML report
open target/gatling/stresstestsimulation-*/index.html
```

**Analysis Points:**

- At what user count does p95 exceed 500ms?
- When does error rate exceed 5%?
- What resource exhausts first (CPU, memory, connections)?

---

### Scenario 3: Spike Test (k6)

**Goal:** Validate system resilience to sudden traffic spikes

**Configuration:**

- Pattern: 100 users → 500 users (30s spike) → 100 users
- Duration: 3 minutes total

**Run Test:**

```bash
cd load-tests/k6

# Run spike test
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env API_KEY=test-api-key-local-dev \
  spike-test.js

# Results saved to: ../results/k6-spike/
```

**Expected Behavior:**

- System handles spike without crashing
- Response times degrade gracefully (p95 may spike to 500-800ms)
- Error rate stays <5%
- Recovery time <30s after spike

---

### Scenario 4: Endurance Test (JMeter)

**Goal:** Detect memory leaks and connection pool exhaustion

**Configuration:**

- Users: 500 concurrent
- Duration: 1 hour
- Monitor: Heap usage, GC activity, connection pool

**Run Test:**

```bash
cd load-tests/jmeter

# Run endurance test
jmeter -n -t endurance-test.jmx \
  -JbaseUrl=http://localhost:8080 \
  -JapiKey=test-api-key-local-dev \
  -Jthreads=500 \
  -Jduration=3600 \
  -l ../results/jmeter-endurance/endurance-results.jtl \
  -e -o ../results/jmeter-endurance/report/
```

**Monitoring During Test:**

```bash
# Monitor JVM heap usage
jstat -gcutil $(pgrep -f "spring-boot") 1000

# Monitor HikariCP connections
watch -n 1 'curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq'

# Monitor Redis memory
watch -n 1 'redis-cli INFO memory | grep used_memory_human'
```

**Red Flags:**

- Heap usage continuously grows (memory leak)
- Old Gen GC frequency increases
- Database connection pool exhaustion
- Response time degradation over time

---

### Scenario 5: Rate Limiting Validation (k6)

**Goal:** Verify rate limiting enforcement accuracy

**Configuration:**

- Test BASIC tier (60 req/min)
- Send 70 requests rapidly
- Expect: First 60 succeed, next 10 get 429

**Run Test:**

```bash
cd load-tests/k6

# Test BASIC tier
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

**Validation:**

- BASIC tier: ~14% of requests get 429 (70-60=10 rejected)
- PREMIUM tier: ~9% of requests get 429 (1100-1000=100 rejected)
- Retry-After header present in 429 responses
- Rate limits reset every minute

---

## 🔬 Comparative Benchmarks

### Test A: Redis Cache Enabled vs Disabled

**Test A1 - Cache Enabled (Baseline):**

```bash
# Ensure Redis is running
docker-compose up -d redis

# Run baseline test
cd load-tests/jmeter
jmeter -n -t baseline-test.jmx \
  -JbaseUrl=http://localhost:8080 \
  -JapiKey=test-api-key-local-dev \
  -l ../results/cache-enabled.jtl
```

**Test A2 - Cache Disabled:**

```bash
# Option 1: Stop Redis
docker-compose stop redis

# Option 2: Disable caching in application.yml
# spring.cache.type: none
# Then restart application

# Run same test
cd load-tests/jmeter
jmeter -n -t baseline-test.jmx \
  -JbaseUrl=http://localhost:8080 \
  -JapiKey=test-api-key-local-dev \
  -l ../results/cache-disabled.jtl
```

**Expected Comparison:**

- **Cache Enabled:** p95 <100ms, DB queries: ~10 (cache hit rate 95%+)
- **Cache Disabled:** p95 200-300ms, DB queries: ~10,000 (every request hits DB)
- **Throughput Improvement:** 3-5x with caching

---

## 📊 Monitoring & Realtime Metrics

**Xem metrics realtime khi load testing:**

- **[MONITORING_WHILE_TESTING.md](./MONITORING_WHILE_TESTING.md)** - Hướng dẫn xem realtime metrics
- **[../monitoring/QUICK_MONITORING.md](../monitoring/QUICK_MONITORING.md)** - Quick start monitoring
- **[../monitoring/MONITORING_GUIDE.md](../monitoring/MONITORING_GUIDE.md)** - Complete monitoring guide

**Quick start:**

```bash
# 1. Khởi động monitoring
docker-compose -f ../docker-compose-monitoring.yml up -d

# 2. Mở Grafana
open http://localhost:3000  # admin/admin

# 3. Chạy test và xem metrics realtime
```

---

## 📈 Results Collection

### Automated Metrics Collection

Use the collection script to gather metrics during tests:

```bash
# Start metrics collection
./scripts/collect-metrics.sh baseline-test 600 &

# Run your test
jmeter -n -t baseline-test.jmx ...

# Metrics saved to: results/baseline-test-metrics-*.txt
```

### Manual Metrics Collection

**Prometheus Metrics:**

```bash
# Export Prometheus metrics
curl http://localhost:8080/actuator/prometheus > results/prometheus-metrics.txt
```

**Grafana Dashboard:**

- Access: http://localhost:3000
- Login: admin/admin
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

## 📊 Results Analysis

### JMeter Results

**View HTML Report:**

```bash
open load-tests/results/jmeter-baseline/report/index.html
```

**Key Metrics to Review:**

- Response Times (p50, p95, p99)
- Throughput (requests/sec)
- Error Rate (%)
- Response Codes Distribution

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

- http_req_duration (p50, p95, p99)
- http_reqs (total requests, rate)
- http_req_failed (error rate)

---

## 📝 Report Generation

### Generate Performance Reports

```bash
# Generate baseline report
./scripts/generate-report.sh baseline

# Generate stress test report
./scripts/generate-report.sh stress

# Generate comparative analysis
./scripts/generate-report.sh comparative
```

Reports are saved to: `plans/reports/`

---

## 🎯 Success Criteria

### Functional

- ✅ All load test scripts execute successfully
- ✅ No errors during baseline test (<0.1% error rate)
- ✅ Rate limiting enforced correctly (99.9% accuracy)

### Performance Targets

- **Response Time (p95):** <200ms for GET requests
- **Throughput:** >1,000 req/sec sustained
- **Cache Hit Rate:** >90% for cached endpoints
- **Error Rate:** <1% under stress test
- **Recovery Time:** <30s after spike test

---

## 📁 Directory Structure

```
load-tests/
├── README.md                    # This file
├── jmeter/
│   ├── baseline-test.jmx       # Baseline performance test
│   ├── endurance-test.jmx      # 1-hour endurance test
│   └── README.md               # JMeter-specific guide
├── gatling/
│   ├── pom.xml                 # Gatling Maven project
│   ├── src/test/scala/
│   │   └── StressTestSimulation.scala
│   └── README.md               # Gatling-specific guide
├── k6/
│   ├── spike-test.js           # Spike test
│   ├── rate-limit-test.js      # Rate limit validation
│   └── README.md               # k6-specific guide
├── results/
│   ├── jmeter-baseline/        # JMeter baseline results
│   ├── jmeter-endurance/       # JMeter endurance results
│   ├── gatling-stress/         # Gatling stress test results
│   ├── k6-spike/               # k6 spike test results
│   └── k6-rate-limit/          # k6 rate limit results
└── scripts/
    ├── collect-metrics.sh      # Metrics collection script
    └── generate-report.sh      # Report generation script
```

---

## 🚀 Quick Start

**Run all tests in sequence:**

```bash
# 1. Baseline test (10 min)
cd load-tests/jmeter
jmeter -n -t baseline-test.jmx -l ../results/jmeter-baseline/results.jtl -e -o ../results/jmeter-baseline/report/

# 2. Stress test (15 min)
cd ../gatling
mvn gatling:test

# 3. Spike test (3 min)
cd ../k6
k6 run spike-test.js

# 4. Rate limit test (1 min)
k6 run rate-limit-test.js

# 5. Endurance test (1 hour - optional)
cd ../jmeter
jmeter -n -t endurance-test.jmx -l ../results/jmeter-endurance/results.jtl
```

**Total time:** ~30 minutes (excluding endurance test)

---

## 📚 Additional Resources

- [JMeter Documentation](https://jmeter.apache.org/usermanual/)
- [Gatling Documentation](https://gatling.io/docs/gatling/)
- [k6 Documentation](https://k6.io/docs/)
- [Performance Testing Best Practices](./docs/load-testing-guide.md)

---

**Status:** Phase 9 Implementation Complete ✅
