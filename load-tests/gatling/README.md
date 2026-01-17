# Gatling Load Tests

Gatling stress tests for finding the breaking point of the Scalable Spring Boot API.

## Prerequisites

### Installation

**Required: Maven 3.9+**

```bash
# macOS (Homebrew) - Recommended
brew install maven

# Verify installation
mvn --version
```

**Requirements:**
- ✅ Java 21 (LTS) - Already installed
- ⚠️ Maven 3.9+ - **Need to install** (see [INSTALL.md](./INSTALL.md))
- ✅ Scala 2.13 - Auto-installed via Maven

**Chi tiết cài đặt:** Xem [INSTALL.md](./INSTALL.md)

## Test Scripts

### Stress Test Simulation (`StressTestSimulation.scala`)

**Scenario:** Gradual load ramp from 100 → 1000 users to find breaking point

**Load Profile:**
- Phase 1: Warm-up (100 users) - 1 minute
- Phase 2: Normal load (200 users) - 1 minute
- Phase 3: Increased load (400 users) - 1 minute
- Phase 4: Heavy load (600 users) - 1 minute
- Phase 5: Stress load (800 users) - 1 minute
- Phase 6: Breaking point (1000 users) - 1 minute

**Total Duration:** ~6 minutes

**Performance Targets:**
- p95 response time: <500ms (acceptable under stress)
- Success rate: >99%
- Find breaking point: Load level where error rate >1%

## Running Tests

### Maven Mode (Recommended)

```bash
# Navigate to Gatling directory
cd load-tests/gatling

# Compile simulation
mvn clean compile

# Run stress test with defaults
mvn gatling:test

# Run with custom parameters
mvn gatling:test \
  -DbaseUrl=http://localhost:8080 \
  -DapiKey=test-api-key-local-dev
```

### Standalone Mode

```bash
# If using standalone Gatling installation
cd load-tests/gatling
gatling -s simulations.StressTestSimulation
```

## Test Execution Steps

### 1. Pre-Test Preparation

```bash
# Verify API health
curl -H "X-API-Key: test-api-key-local-dev" http://localhost:8080/actuator/health

# Monitor resources before test
docker stats --no-stream

# Clear Redis cache for fresh test
docker exec scalable-api-redis redis-cli FLUSHDB

# Start monitoring (separate terminal)
watch -n 1 'docker stats --no-stream'
```

### 2. Execute Stress Test

```bash
cd load-tests/gatling

# Run stress test (6-7 minutes)
mvn gatling:test -DbaseUrl=http://localhost:8080 -DapiKey=test-api-key-local-dev

# Expected output:
# ================================================================================
# Simulation simulations.StressTestSimulation completed in 6 minutes
# ================================================================================
# > request count                                      50000 (OK=49500  KO=500)
# > min response time                                     10 (OK=10     KO=5010)
# > max response time                                   2500 (OK=1500   KO=5010)
# > mean response time                                    85 (OK=80     KO=5005)
# > std deviation                                        150 (OK=100    KO=5)
# > response time 50th percentile                         50 (OK=45     KO=5010)
# > response time 75th percentile                        100 (OK=90     KO=5010)
# > response time 95th percentile                        350 (OK=300    KO=5010)
# > response time 99th percentile                        800 (OK=700    KO=5010)
# > mean requests/sec                                 138.89 (OK=137.50 KO=1.39)
# ================================================================================
```

###3. Analyze Results

```bash
# HTML report auto-generated in target/gatling/
ls -lh target/gatling/stresstestsimulation-*/

# Open report in browser
open target/gatling/stresstestsimulation-*/index.html

# Copy results to project results directory
cp -r target/gatling/stresstestsimulation-* ../../results/gatling-stress/
```

## Understanding Results

### HTML Report Sections

1. **Global Menu** - Overview statistics
2. **Details** - Per-request breakdown
3. **Charts** - Response time, throughput, active users
4. **Metrics** - Percentiles, error analysis

### Key Metrics

**Breaking Point Identification:**
```
Phase | Users | Req/sec | p95 (ms) | Error Rate | Status
------|-------|---------|----------|------------|--------
  1   |  100  |   50    |   100    |   0.0%     | Normal
  2   |  200  |  100    |   150    |   0.1%     | Normal
  3   |  400  |  180    |   250    |   0.3%     | Acceptable
  4   |  600  |  240    |   400    |   0.8%     | Stress
  5   |  800  |  280    |   650    |   1.5%     | Breaking Point ⚠️
  6   | 1000  |  250    |  1200    |   5.0%     | Degraded ❌
```

**Breaking Point:** ~800 concurrent users (error rate crosses 1% threshold)

### Performance Analysis

```bash
# Extract key metrics from Gatling report
cd target/gatling/stresstestsimulation-*/
grep "response time 95th percentile" simulation.log
grep "mean requests/sec" simulation.log
grep "request count" simulation.log | grep KO | awk '{print $NF}'
```

## Troubleshooting

### Common Issues

**1. Compilation Errors**
```bash
# Error: Cannot resolve scala dependencies
# Solution: Clean and re-compile
mvn clean install
```

**2. Connection Refused**
```bash
# Error: j.n.ConnectException: Connection refused
# Solution: Verify API is running on correct port
curl http://localhost:8080/actuator/health
```

**3. Gatling Timeout**
```bash
# Error: Request timeout after 60000ms
# Solution: Increase timeout in simulation or reduce load
# Edit StressTestSimulation.scala:
# .check(status.in(200).maxDuration(120.seconds))
```

**4. Out of Memory**
```bash
# Error: java.lang.OutOfMemoryError
# Solution: Increase Maven heap size
export MAVEN_OPTS="-Xms1024m -Xmx2048m -XX:+UseG1GC"
mvn gatling:test
```

## Best Practices

1. **Monitor API server** during test (CPU, memory, DB connections)
2. **Start with low load** (100 users) and gradually increase
3. **Watch for connection pool exhaustion** in HikariCP metrics
4. **Check error logs** for 500 errors (indicates server issues)
5. **Compare with baseline** (JMeter results) for consistency
6. **Document breaking point** clearly in report

## Performance Tuning

### Gatling Configuration

**Edit `gatling.conf` (if using standalone):**
```hocon
gatling {
  http {
    maxConnectionsPerHost = 10
    requestTimeout = 60000
    pooledConnectionIdleTimeout = 60000
  }
}
```

### JVM Tuning

```bash
# Increase heap for large simulations
export MAVEN_OPTS="-Xms2048m -Xmx4096m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

## Next Steps

1. Execute stress test and identify breaking point
2. Analyze resource utilization (CPU, memory, DB connections)
3. Document bottlenecks (connection pool, Redis, DB queries)
4. Proceed to k6 spike test
5. Create performance-stress-test-report.md

## References

- [Gatling Documentation](https://gatling.io/docs/gatling/)
- [Gatling Maven Plugin](https://gatling.io/docs/gatling/reference/current/extensions/maven_plugin/)
- [Scala DSL Reference](https://gatling.io/docs/gatling/reference/current/core/simulation/)
