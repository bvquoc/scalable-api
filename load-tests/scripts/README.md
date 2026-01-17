# Load Test Scripts

Utility scripts for load testing execution and results collection.

## Scripts

### `collect-metrics.sh`

Automated metrics collection during load tests.

**Usage:**
```bash
./collect-metrics.sh <test-name> <duration-seconds> [interval-seconds]
```

**Example:**
```bash
# Collect metrics every 10 seconds for 10 minutes
./collect-metrics.sh baseline-test 600 10
```

**What it collects:**
- Application health status
- Prometheus metrics (HTTP, HikariCP, JVM)
- HikariCP connection pool stats
- JVM memory stats (via jstat)
- Redis memory usage
- System load (Linux only)

**Output:**
- Metrics files: `load-tests/results/<test-name>/metrics/metrics-*.txt`
- Summary: `load-tests/results/<test-name>/metrics/summary.txt`

---

### `compare-cache.sh`

Compare performance with Redis cache enabled vs disabled.

**Usage:**
```bash
./compare-cache.sh
```

**What it does:**
1. Runs baseline test with Redis enabled
2. Stops Redis
3. Runs same test with Redis disabled
4. Generates comparison report

**Output:**
- Cache enabled results: `load-tests/results/cache-comparison/cache-enabled-report/`
- Cache disabled results: `load-tests/results/cache-comparison/cache-disabled-report/`
- Comparison report: `load-tests/results/cache-comparison/cache-comparison-report.md`

---

### `generate-report.sh`

Generate performance test reports from results.

**Usage:**
```bash
./generate-report.sh <test-type>
```

**Test types:**
- `baseline` - Baseline performance report
- `stress` - Stress test report
- `spike` - Spike test report
- `endurance` - Endurance test report
- `rate-limit` - Rate limit validation report
- `comparative` - Comparative analysis report

**Example:**
```bash
./generate-report.sh baseline
```

**Output:**
- Report: `plans/reports/performance-<test-type>-report.md`

**Note:** Reports are generated as templates. You need to:
1. Extract actual metrics from test results
2. Update report tables with real values
3. Add analysis and recommendations

---

## Requirements

### System Tools

- `curl` - HTTP requests
- `jq` - JSON parsing
- `jstat` - JVM statistics (part of JDK)
- `redis-cli` - Redis commands
- `docker` / `docker-compose` - Container management

### Application

- Spring Boot application running on `http://localhost:8080`
- Actuator endpoints enabled
- Prometheus metrics exposed

---

## Examples

### Complete Test Run with Metrics Collection

```bash
# Terminal 1: Start metrics collection
cd load-tests
./scripts/collect-metrics.sh baseline-test 600 10

# Terminal 2: Run load test
cd jmeter
jmeter -n -t baseline-test.jmx \
  -JbaseUrl=http://localhost:8080 \
  -JapiKey=test-api-key-local-dev \
  -l ../results/jmeter-baseline/results.jtl \
  -e -o ../results/jmeter-baseline/report/

# After test completes: Generate report
cd ..
./scripts/generate-report.sh baseline
```

### Cache Comparison Test

```bash
cd load-tests
./scripts/compare-cache.sh

# View results
open results/cache-comparison/cache-enabled-report/index.html
open results/cache-comparison/cache-disabled-report/index.html
cat results/cache-comparison/cache-comparison-report.md
```

---

## Troubleshooting

### Script fails with "command not found"

**Solution:**
```bash
# Make scripts executable
chmod +x scripts/*.sh

# Or run with bash
bash scripts/collect-metrics.sh baseline-test 600
```

### Metrics collection fails

**Check:**
1. Application is running: `curl http://localhost:8080/actuator/health`
2. Actuator endpoints enabled
3. Spring Boot PID found: `pgrep -f "spring-boot"`

### Redis commands fail

**Check:**
1. Redis is running: `docker ps | grep redis`
2. Redis CLI available: `redis-cli ping`

---

## Contributing

When adding new scripts:

1. Add shebang: `#!/bin/bash`
2. Add error handling: `set -e`
3. Add usage documentation
4. Make executable: `chmod +x script.sh`
5. Update this README

