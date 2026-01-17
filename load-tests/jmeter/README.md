# JMeter Load Tests

Apache JMeter baseline performance tests for the Scalable Spring Boot API.

## Prerequisites

### Installation

**macOS (Homebrew):**
```bash
brew install jmeter
```

**Linux (APT):**
```bash
sudo apt-get update
sudo apt-get install jmeter
```

**Manual Installation:**
1. Download JMeter 5.6+ from https://jmeter.apache.org/download_jmeter.cgi
2. Extract archive
3. Add `bin/` directory to PATH

**Verify Installation:**
```bash
jmeter --version
# Should show: Apache JMeter 5.6 or higher
```

### Recommended Plugins

Install JMeter Plugin Manager:
1. Download from https://jmeter-plugins.org/install/Install/
2. Place `jmeter-plugins-manager.jar` in `lib/ext/` directory
3. Restart JMeter

**Install via Plugin Manager:**
- Custom Thread Groups
- Throughput Shaping Timer
- PerfMon (Server Agent)
- CSV Data Set Config

## Test Scripts

### 1. Baseline Load Test (`baseline-test.jmx`)

**Scenario:** 100 concurrent users, 10 minute duration, read-heavy workload

**Endpoints Tested:**
- GET `/api/users?page={0-10}&size=20` (40% weight)
- GET `/api/products?page={0-5}&size=20` (40% weight)
- GET `/api/orders/{1-100}` (20% weight)

**Configuration:**
- Threads: 100 concurrent users
- Ramp-up: 60 seconds (gradual start)
- Duration: 600 seconds (10 minutes)
- Think time: 1-3 seconds between requests
- Connection pooling: Keep-alive enabled

**Performance Targets:**
- p50 response time: <50ms
- p95 response time: <200ms
- p99 response time: <500ms
- Throughput: >500 req/sec
- Error rate: <0.1%

## Running Tests

### GUI Mode (Development)

```bash
# Navigate to load-tests directory
cd load-tests/jmeter

# Run JMeter GUI
jmeter -t baseline-test.jmx
```

**GUI Workflow:**
1. Configure test plan variables (optional)
2. Click green "Start" button (▶️)
3. Monitor progress in listeners
4. View results in "Summary Report" and "Aggregate Report"
5. Export results to CSV

### Command-Line Mode (Production)

```bash
# Basic run with defaults
jmeter -n -t baseline-test.jmx -l results/baseline-results.jtl

# Run with custom parameters
jmeter -n -t baseline-test.jmx \
  -JbaseUrl=http://localhost:8080 \
  -JapiKey=test-api-key-local-dev \
  -Jthreads=100 \
  -Jrampup=60 \
  -Jduration=600 \
  -l results/baseline-results.jtl \
  -e -o results/html-report
```

**Parameters:**
- `-n`: Non-GUI mode
- `-t`: Test plan file
- `-J`: JMeter property (overrides test plan variables)
- `-l`: Log file (JTL format)
- `-e`: Generate HTML report after test
- `-o`: Output directory for HTML report

### Docker Mode

```bash
# Using official JMeter Docker image
docker run --rm \
  -v $(pwd):/mnt/jmeter \
  --network="host" \
  justb4/jmeter:5.6 \
  -n -t /mnt/jmeter/baseline-test.jmx \
  -JbaseUrl=http://localhost:8080 \
  -JapiKey=test-api-key-local-dev \
  -l /mnt/jmeter/results/baseline-results.jtl \
  -e -o /mnt/jmeter/results/html-report
```

## Test Execution Steps

### 1. Pre-Test Checklist

```bash
# Verify API is running
curl -H "X-API-Key: test-api-key-local-dev" http://localhost:8080/actuator/health
# Expected: {"status":"UP"}

# Verify Docker infrastructure is running
docker-compose ps
# Expected: All services (postgres, redis, kafka, rabbitmq) healthy

# Check Redis cache (should be empty or stale)
docker exec scalable-api-redis redis-cli FLUSHDB

# Monitor Grafana dashboard (optional)
open http://localhost:3000
```

### 2. Execute Test

```bash
# Create results directory
mkdir -p results/jmeter-baseline

# Run baseline test (10 minutes)
jmeter -n -t baseline-test.jmx \
  -JbaseUrl=http://localhost:8080 \
  -JapiKey=test-api-key-local-dev \
  -l results/jmeter-baseline/baseline-results.jtl \
  -e -o results/jmeter-baseline/html-report

# Expected output:
# summary =    60000 in 00:10:00 =  100.0/s Avg:    50 Min:     10 Max:   200 Err:     0 (0.00%)
```

### 3. Post-Test Analysis

```bash
# Open HTML report in browser
open results/jmeter-baseline/html-report/index.html

# Extract key metrics from JTL file
awk -F',' '{sum+=$2; count++} END {print "Avg Response Time:", sum/count "ms"}' results/jmeter-baseline/baseline-results.jtl

# Count errors
grep ',false,' results/jmeter-baseline/baseline-results.jtl | wc -l

# Export Prometheus/Grafana metrics
curl http://localhost:8080/actuator/prometheus > results/jmeter-baseline/prometheus-metrics.txt
```

## Understanding Results

### JTL File Format

```csv
timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,failureMessage,bytes,sentBytes,grpThreads,allThreads,URL,Latency,IdleTime,Connect
1704643200000,45,GET /api/users,200,OK,Thread Group 1-1,text,true,,1234,567,100,100,http://localhost:8080/api/users,40,0,5
```

**Key Columns:**
- `elapsed`: Response time in milliseconds
- `responseCode`: HTTP status code (200, 404, 429, 500, etc.)
- `success`: true/false (whether request succeeded)
- `bytes`: Response size
- `Latency`: Time to first byte
- `Connect`: Connection establishment time

### HTML Report Sections

1. **Dashboard** - Overview with key metrics
2. **Statistics** - Request counts, response times, throughput
3. **Response Times Over Time** - Latency graph
4. **Response Times Percentiles** - p50, p90, p95, p99
5. **Transactions Per Second** - Throughput graph
6. **Response Time vs Request** - Scatter plot

### Performance Metrics

**Response Time Analysis:**
```bash
# Extract percentiles from JTL
sort -t',' -k2 -n results/jmeter-baseline/baseline-results.jtl | \
  awk -F',' '{print $2}' | \
  awk '{
    a[NR]=$1
  } END {
    print "p50:", a[int(NR*0.50)]
    print "p95:", a[int(NR*0.95)]
    print "p99:", a[int(NR*0.99)]
  }'
```

## Troubleshooting

### Common Issues

**1. Connection Timeout**
```bash
# Error: Connection timeout after 60000ms
# Solution: Increase connection timeout in test plan or verify API is running
curl http://localhost:8080/actuator/health
```

**2. Rate Limited (429)**
```bash
# Error: 429 Too Many Requests
# Solution: Use PREMIUM tier API key or reduce thread count
# Verify rate limit tier in database:
docker exec scalable-api-postgres psql -U postgres -d apidb_dev \
  -c "SELECT name, rate_limit_tier FROM api_keys WHERE key_hash='...';"
```

**3. Out of Memory**
```bash
# Error: java.lang.OutOfMemoryError
# Solution: Increase JMeter heap size
jmeter -Xms1024m -Xmx2048m -n -t baseline-test.jmx ...
```

**4. No Results Collected**
```bash
# Issue: Empty JTL file or no HTML report
# Solution: Check file permissions and disk space
ls -lh results/jmeter-baseline/
df -h
```

## Best Practices

1. **Always run tests in non-GUI mode** for production load testing (GUI consumes significant resources)
2. **Use CSV output (JTL)** instead of XML for better performance with large result sets
3. **Disable unnecessary listeners** in test plan (they consume memory)
4. **Monitor JMeter process** during test (CPU, memory, network)
5. **Run JMeter on separate machine** from API server for accurate results
6. **Warm up cache before test** if testing with Redis (run 1-2 min warm-up first)
7. **Document test environment** (hardware, network, OS) for reproducibility

## Performance Tuning

### JMeter Configuration

**Edit `jmeter.properties`:**
```properties
# Increase heap size for large tests
-Xms1024m
-Xmx2048m

# Disable summary reporting during test (improves performance)
summariser.interval=0

# Increase HTTP connection pool
httpclient4.retrycount=0
httpclient4.idletimeout=60000
httpclient4.validate_after_inactivity=1000
```

### System Tuning

**macOS/Linux:**
```bash
# Increase open file limit
ulimit -n 10000

# Check current limits
ulimit -a
```

## Next Steps

1. Run baseline test with Redis enabled (Test A)
2. Run baseline test with Redis disabled (Test B)
3. Compare results to quantify caching impact
4. Proceed to Gatling stress test
5. Document findings in performance-baseline-report.md

## References

- [JMeter User Manual](https://jmeter.apache.org/usermanual/index.html)
- [JMeter Best Practices](https://jmeter.apache.org/usermanual/best-practices.html)
- [JMeter Plugins](https://jmeter-plugins.org/)
