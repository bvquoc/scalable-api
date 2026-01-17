#!/bin/bash
# generate-report.sh - Generate performance test reports
#
# Usage:
#   ./generate-report.sh <test-type>
#
# Test types:
#   - baseline: Baseline performance report
#   - stress: Stress test report
#   - spike: Spike test report
#   - endurance: Endurance test report
#   - rate-limit: Rate limit validation report
#   - comparative: Comparative analysis report

set -e

TEST_TYPE=${1:-"baseline"}
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORTS_DIR="${BASE_DIR}/../../plans/reports"
RESULTS_DIR="${BASE_DIR}/results"

# Create reports directory
mkdir -p "${REPORTS_DIR}"

echo "Generating ${TEST_TYPE} report..."

case "${TEST_TYPE}" in
  baseline)
    REPORT_FILE="${REPORTS_DIR}/performance-baseline-report.md"
    RESULTS_PATH="${RESULTS_DIR}/jmeter-baseline"
    ;;
  stress)
    REPORT_FILE="${REPORTS_DIR}/performance-stress-test-report.md"
    RESULTS_PATH="${RESULTS_DIR}/gatling-stress"
    ;;
  spike)
    REPORT_FILE="${REPORTS_DIR}/performance-spike-test-report.md"
    RESULTS_PATH="${RESULTS_DIR}/k6-spike"
    ;;
  endurance)
    REPORT_FILE="${REPORTS_DIR}/performance-endurance-test-report.md"
    RESULTS_PATH="${RESULTS_DIR}/jmeter-endurance"
    ;;
  rate-limit)
    REPORT_FILE="${REPORTS_DIR}/rate-limit-validation-report.md"
    RESULTS_PATH="${RESULTS_DIR}/k6-rate-limit"
    ;;
  comparative)
    REPORT_FILE="${REPORTS_DIR}/performance-comparative-analysis.md"
    RESULTS_PATH="${RESULTS_DIR}/cache-comparison"
    ;;
  *)
    echo "Unknown test type: ${TEST_TYPE}"
    echo "Valid types: baseline, stress, spike, endurance, rate-limit, comparative"
    exit 1
    ;;
esac

# Generate report template
{
  echo "# Performance Test Report: ${TEST_TYPE^}"
  echo ""
  echo "**Generated:** $(date)"
  echo "**Test Type:** ${TEST_TYPE}"
  echo ""
  echo "## Test Configuration"
  echo ""
  echo "| Parameter | Value |"
  echo "|-----------|-------|"
  echo "| Test Date | $(date +%Y-%m-%d) |"
  echo "| Test Time | $(date +%H:%M:%S) |"
  echo "| Results Path | \`${RESULTS_PATH}\` |"
  echo ""
  echo "## Results Summary"
  echo ""
  echo "### Response Time Metrics"
  echo ""
  echo "| Metric | Value | Target | Status |"
  echo "|--------|-------|--------|--------|"
  echo "| p50 | TBD | <100ms | TBD |"
  echo "| p95 | TBD | <200ms | TBD |"
  echo "| p99 | TBD | <500ms | TBD |"
  echo "| Max | TBD | - | - |"
  echo "| Mean | TBD | - | - |"
  echo ""
  echo "### Throughput"
  echo ""
  echo "| Metric | Value | Target | Status |"
  echo "|--------|-------|--------|--------|"
  echo "| Requests/sec | TBD | >1,000 | TBD |"
  echo "| Total Requests | TBD | - | - |"
  echo "| Success Rate | TBD | >99% | TBD |"
  echo "| Error Rate | TBD | <1% | TBD |"
  echo ""
  echo "### Resource Utilization"
  echo ""
  echo "| Resource | Average | Peak |"
  echo "|----------|---------|------|"
  echo "| CPU Usage | TBD | TBD |"
  echo "| Memory (Heap) | TBD | TBD |"
  echo "| DB Connections | TBD | TBD |"
  echo "| Redis Connections | TBD | TBD |"
  echo ""
  echo "## Analysis"
  echo ""
  echo "### Key Findings"
  echo ""
  echo "- TBD"
  echo ""
  echo "### Bottlenecks Identified"
  echo ""
  echo "- TBD"
  echo ""
  echo "### Recommendations"
  echo ""
  echo "- TBD"
  echo ""
  echo "## Test Artifacts"
  echo ""
  echo "- Results: \`${RESULTS_PATH}\`"
  echo "- Metrics: See metrics collection files"
  echo "- Logs: See test execution logs"
  echo ""
  echo "## Next Steps"
  echo ""
  echo "1. Review detailed results in \`${RESULTS_PATH}\`"
  echo "2. Update this report with actual metrics"
  echo "3. Compare with previous test runs"
  echo "4. Implement recommendations"
  echo ""

} > "${REPORT_FILE}"

echo "Report generated: ${REPORT_FILE}"
echo ""
echo "Next steps:"
echo "1. Review test results in: ${RESULTS_PATH}"
echo "2. Extract metrics and update the report"
echo "3. Add analysis and recommendations"

