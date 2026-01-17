#!/bin/bash
# collect-metrics.sh - Automated metrics collection during load tests
#
# Usage:
#   ./collect-metrics.sh <test-name> <duration-seconds> [interval-seconds]
#
# Example:
#   ./collect-metrics.sh baseline-test 600 10
#   # Collects metrics every 10 seconds for 10 minutes (600 seconds)

set -e

TEST_NAME=${1:-"unknown-test"}
DURATION=${2:-600}
INTERVAL=${3:-10}

RESULTS_DIR="load-tests/results/${TEST_NAME}"
METRICS_DIR="${RESULTS_DIR}/metrics"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

# Create results directory
mkdir -p "${METRICS_DIR}"

echo "=========================================="
echo "Metrics Collection Started"
echo "=========================================="
echo "Test Name: ${TEST_NAME}"
echo "Duration: ${DURATION} seconds"
echo "Interval: ${INTERVAL} seconds"
echo "Results Dir: ${METRICS_DIR}"
echo "=========================================="
echo ""

# Find Spring Boot process
SPRING_BOOT_PID=$(pgrep -f "spring-boot" || echo "")
if [ -z "${SPRING_BOOT_PID}" ]; then
  echo "WARNING: Spring Boot process not found. Some metrics may not be available."
fi

# Function to collect metrics
collect_metrics() {
  local iteration=$1
  local timestamp=$(date +%Y-%m-%dT%H:%M:%S)
  local metrics_file="${METRICS_DIR}/metrics-${timestamp}.txt"

  echo "[${timestamp}] Collecting metrics (iteration ${iteration})..."

  {
    echo "=== Metrics Collection ${timestamp} ==="
    echo ""

    # Application Health
    echo "--- Application Health ---"
    curl -s http://localhost:8080/actuator/health 2>/dev/null | jq . || echo "Health endpoint unavailable"
    echo ""

    # Prometheus Metrics (key metrics only)
    echo "--- Key Prometheus Metrics ---"
    curl -s http://localhost:8080/actuator/prometheus 2>/dev/null | grep -E "(http_server_requests|hikaricp|jvm_memory|process_cpu)" || echo "Prometheus endpoint unavailable"
    echo ""

    # HikariCP Connection Pool
    echo "--- HikariCP Connection Pool ---"
    curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active 2>/dev/null | jq . || echo "HikariCP metrics unavailable"
    curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.idle 2>/dev/null | jq . || echo ""
    curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.pending 2>/dev/null | jq . || echo ""
    echo ""

    # JVM Memory
    echo "--- JVM Memory ---"
    if [ -n "${SPRING_BOOT_PID}" ]; then
      jstat -gcutil ${SPRING_BOOT_PID} 2>/dev/null || echo "JVM stats unavailable"
    else
      echo "Spring Boot PID not found"
    fi
    echo ""

    # Redis Memory (if available)
    echo "--- Redis Memory ---"
    redis-cli INFO memory 2>/dev/null | grep -E "(used_memory_human|used_memory_peak_human|mem_fragmentation_ratio)" || echo "Redis unavailable"
    echo ""

    # System Load (if on Linux)
    if [ "$(uname)" = "Linux" ]; then
      echo "--- System Load ---"
      uptime
      echo ""
    fi

  } > "${metrics_file}"

  echo "Metrics saved to: ${metrics_file}"
}

# Collect initial metrics
collect_metrics 0

# Calculate number of iterations
ITERATIONS=$((DURATION / INTERVAL))

# Collect metrics at intervals
for i in $(seq 1 ${ITERATIONS}); do
  sleep ${INTERVAL}
  collect_metrics ${i}
done

# Collect final metrics
echo ""
echo "Collecting final metrics..."
collect_metrics "final"

# Generate summary
SUMMARY_FILE="${METRICS_DIR}/summary.txt"
{
  echo "=========================================="
  echo "Metrics Collection Summary"
  echo "=========================================="
  echo "Test Name: ${TEST_NAME}"
  echo "Start Time: $(date -r ${METRICS_DIR}/metrics-*.txt | head -1)"
  echo "End Time: $(date)"
  echo "Duration: ${DURATION} seconds"
  echo "Total Collections: $((ITERATIONS + 2))"
  echo "=========================================="
  echo ""
  echo "Metrics files:"
  ls -lh "${METRICS_DIR}"/*.txt | tail -n +2
} > "${SUMMARY_FILE}"

echo ""
echo "=========================================="
echo "Metrics Collection Complete"
echo "=========================================="
echo "Results saved to: ${METRICS_DIR}"
echo "Summary: ${SUMMARY_FILE}"
echo "=========================================="

