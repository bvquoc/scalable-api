# Phase 9: Quick Start Guide

**Get started with load testing in 5 minutes**

## Prerequisites Check

```bash
# 1. Check if tools are installed
jmeter --version    # Should show JMeter 5.6+
k6 version          # Should show k6 v0.48.0+
mvn --version       # Should show Maven 3.9+

# 2. Check if services are running
docker-compose ps   # PostgreSQL, Redis, RabbitMQ, Kafka should be "Up"
curl http://localhost:8080/actuator/health  # Should return {"status":"UP"}
```

## Quick Test: Baseline Performance (10 minutes)

**Step 1: Start metrics collection (optional)**
```bash
cd load-tests
./scripts/collect-metrics.sh baseline-test 600 10 &
```

**Step 2: Run baseline test**
```bash
cd jmeter
jmeter -n -t baseline-test.jmx \
  -JbaseUrl=http://localhost:8080 \
  -JapiKey=test-api-key-local-dev \
  -Jthreads=100 \
  -Jrampup=60 \
  -Jduration=600 \
  -l ../results/jmeter-baseline/results.jtl \
  -e -o ../results/jmeter-baseline/report/
```

**Step 3: View results**
```bash
open ../results/jmeter-baseline/report/index.html
```

## Quick Test: Rate Limiting (1 minute)

```bash
cd load-tests/k6
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env API_KEY=test-api-key-local-dev \
  --env TIER=BASIC \
  rate-limit-test.js
```

Expected output:
- First 60 requests: ✅ 200 OK
- Request 61+: ❌ 429 Too Many Requests

## Quick Test: Spike Test (3 minutes)

```bash
cd load-tests/k6
k6 run \
  --env BASE_URL=http://localhost:8080 \
  --env API_KEY=test-api-key-local-dev \
  spike-test.js
```

Watch for:
- System handles spike gracefully
- Error rate stays <1%
- Recovery time <30s

## Cache Comparison Test (15 minutes)

```bash
cd load-tests
./scripts/compare-cache.sh
```

This will:
1. Run test with Redis enabled
2. Stop Redis
3. Run same test with Redis disabled
4. Generate comparison report

## Generate Reports

```bash
cd load-tests
./scripts/generate-report.sh baseline
./scripts/generate-report.sh stress
./scripts/generate-report.sh comparative
```

Reports saved to: `plans/reports/`

## Common Issues

**Issue: JMeter not found**
```bash
# macOS
brew install jmeter

# Or add to PATH
export PATH=$PATH:/path/to/apache-jmeter-5.6.3/bin
```

**Issue: Application not responding**
```bash
# Check if app is running
curl http://localhost:8080/actuator/health

# Restart application
mvn spring-boot:run
```

**Issue: Redis connection error**
```bash
# Start Redis
docker-compose up -d redis

# Verify
redis-cli ping  # Should return PONG
```

## Next Steps

1. **Read full guide:** [README.md](./README.md)
2. **Run all tests:** Follow execution guide
3. **Analyze results:** Review generated reports
4. **Compare metrics:** Use comparative analysis

---

**Need help?** Check the main [README.md](./README.md) for detailed instructions.

