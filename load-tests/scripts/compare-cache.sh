#!/bin/bash
# compare-cache.sh - Compare performance with Redis cache enabled vs disabled
#
# Usage:
#   ./compare-cache.sh
#
# This script:
# 1. Runs baseline test with Redis enabled
# 2. Stops Redis
# 3. Runs same test with Redis disabled
# 4. Generates comparison report

set -e

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JMETER_DIR="${BASE_DIR}/jmeter"
RESULTS_DIR="${BASE_DIR}/results/cache-comparison"

echo "=========================================="
echo "Cache Comparison Test"
echo "=========================================="
echo ""

# Create results directory
mkdir -p "${RESULTS_DIR}"

# Test configuration
BASE_URL="http://localhost:8080"
API_KEY="test-api-key-local-dev"
THREADS=200
DURATION=300  # 5 minutes
RAMPUP=60

# Function to check if Redis is running
check_redis() {
  docker ps | grep -q redis || redis-cli ping > /dev/null 2>&1
}

# Function to check if application is running
check_app() {
  curl -s http://localhost:8080/actuator/health > /dev/null 2>&1
}

# Verify application is running
if ! check_app; then
  echo "ERROR: Application is not running at ${BASE_URL}"
  echo "Please start the application first: mvn spring-boot:run"
  exit 1
fi

echo "Step 1: Running baseline test WITH Redis cache enabled..."
echo "--------------------------------------------------------"

# Ensure Redis is running
if ! check_redis; then
  echo "Starting Redis..."
  cd "${BASE_DIR}/../.."
  docker-compose up -d redis
  sleep 5
fi

# Run test with cache enabled
cd "${JMETER_DIR}"
jmeter -n -t baseline-test.jmx \
  -JbaseUrl=${BASE_URL} \
  -JapiKey=${API_KEY} \
  -Jthreads=${THREADS} \
  -Jrampup=${RAMPUP} \
  -Jduration=${DURATION} \
  -l "${RESULTS_DIR}/cache-enabled.jtl" \
  -e -o "${RESULTS_DIR}/cache-enabled-report" \
  2>&1 | tee "${RESULTS_DIR}/cache-enabled.log"

echo ""
echo "Step 2: Stopping Redis cache..."
echo "--------------------------------------------------------"

# Stop Redis
cd "${BASE_DIR}/../.."
docker-compose stop redis
sleep 3

echo "Redis stopped. Waiting 10 seconds for cache to expire..."
sleep 10

echo ""
echo "Step 3: Running same test WITHOUT Redis cache..."
echo "--------------------------------------------------------"

# Run test with cache disabled
cd "${JMETER_DIR}"
jmeter -n -t baseline-test.jmx \
  -JbaseUrl=${BASE_URL} \
  -JapiKey=${API_KEY} \
  -Jthreads=${THREADS} \
  -Jrampup=${RAMPUP} \
  -Jduration=${DURATION} \
  -l "${RESULTS_DIR}/cache-disabled.jtl" \
  -e -o "${RESULTS_DIR}/cache-disabled-report" \
  2>&1 | tee "${RESULTS_DIR}/cache-disabled.log"

echo ""
echo "Step 4: Restarting Redis..."
echo "--------------------------------------------------------"

# Restart Redis
cd "${BASE_DIR}/../.."
docker-compose start redis

echo ""
echo "Step 5: Generating comparison report..."
echo "--------------------------------------------------------"

# Generate comparison report
COMPARISON_REPORT="${RESULTS_DIR}/cache-comparison-report.md"

{
  echo "# Cache Comparison Test Report"
  echo ""
  echo "**Date:** $(date)"
  echo "**Test Configuration:**"
  echo "- Users: ${THREADS} concurrent"
  echo "- Duration: ${DURATION} seconds (${DURATION} minutes)"
  echo "- Ramp-up: ${RAMPUP} seconds"
  echo ""
  echo "## Results Summary"
  echo ""
  echo "### With Redis Cache (Enabled)"
  echo ""
  if [ -f "${RESULTS_DIR}/cache-enabled-report/index.html" ]; then
    echo "HTML Report: ${RESULTS_DIR}/cache-enabled-report/index.html"
  fi
  echo ""
  echo "Key Metrics (from JMeter Aggregate Report):"
  echo "- Extract from: ${RESULTS_DIR}/cache-enabled.jtl"
  echo ""
  echo "### Without Redis Cache (Disabled)"
  echo ""
  if [ -f "${RESULTS_DIR}/cache-disabled-report/index.html" ]; then
    echo "HTML Report: ${RESULTS_DIR}/cache-disabled-report/index.html"
  fi
  echo ""
  echo "Key Metrics (from JMeter Aggregate Report):"
  echo "- Extract from: ${RESULTS_DIR}/cache-disabled.jtl"
  echo ""
  echo "## Comparison"
  echo ""
  echo "| Metric | Cache Enabled | Cache Disabled | Improvement |"
  echo "|--------|---------------|----------------|-------------|"
  echo "| p95 Latency | TBD | TBD | TBD |"
  echo "| Throughput | TBD | TBD | TBD |"
  echo "| Error Rate | TBD | TBD | TBD |"
  echo ""
  echo "## Analysis"
  echo ""
  echo "### Expected Improvements with Caching:"
  echo "- 3-5x throughput improvement"
  echo "- 50-70% latency reduction"
  echo "- 99%+ reduction in database queries"
  echo "- Lower CPU usage"
  echo ""
  echo "## Next Steps"
  echo ""
  echo "1. Review HTML reports:"
  echo "   - Cache Enabled: ${RESULTS_DIR}/cache-enabled-report/index.html"
  echo "   - Cache Disabled: ${RESULTS_DIR}/cache-disabled-report/index.html"
  echo ""
  echo "2. Extract metrics from JTL files using JMeter GUI or command line tools"
  echo ""
  echo "3. Update this report with actual metrics"
  echo ""

} > "${COMPARISON_REPORT}"

echo ""
echo "=========================================="
echo "Cache Comparison Test Complete"
echo "=========================================="
echo ""
echo "Results:"
echo "  - Cache Enabled: ${RESULTS_DIR}/cache-enabled-report/"
echo "  - Cache Disabled: ${RESULTS_DIR}/cache-disabled-report/"
echo "  - Comparison Report: ${COMPARISON_REPORT}"
echo ""
echo "View reports:"
echo "  open ${RESULTS_DIR}/cache-enabled-report/index.html"
echo "  open ${RESULTS_DIR}/cache-disabled-report/index.html"
echo ""

