/**
 * k6 Spike Test: Sudden traffic spike to validate system resilience
 *
 * Pattern: 100 users → 500 users (30s spike) → 100 users
 * Duration: 3 minutes total
 *
 * Run with:
 *   k6 run --env BASE_URL=http://localhost:8080 --env API_KEY=test-api-key-local-dev spike-test.js
 *
 * Success Criteria:
 * - Error rate < 1% during spike (excluding rate limiting 429)
 * - Rate limiting expected during spike (429 responses are OK)
 * - Recovery time < 30s after spike
 * - p95 response time < 500ms
 *
 * Note: With 500 users and PREMIUM tier (1000 req/min), rate limiting is expected.
 * This test validates that the system handles spikes gracefully with rate limiting.
 */

import http from "k6/http";
import { check, sleep } from "k6";
import { Rate, Trend } from "k6/metrics";

// Configuration
const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const API_KEY = __ENV.API_KEY || "test-api-key-local-dev";

// Custom metrics
const errorRate = new Rate("errors");
const productLatency = new Trend("product_latency");
const userLatency = new Trend("user_latency");

// Load stages: Spike pattern
export const options = {
  stages: [
    { duration: "1m", target: 100 }, // Ramp to baseline (100 users)
    { duration: "30s", target: 500 }, // Sudden spike to 500 users
    { duration: "1m", target: 100 }, // Recovery to baseline
    { duration: "30s", target: 0 }, // Ramp down
  ],
  thresholds: {
    http_req_duration: ["p(95)<500"], // p95 < 500ms
    // Note: http_req_failed includes 429 (rate limiting), which is expected during spike
    // We use custom 'errors' metric that excludes 429
    http_req_failed: ["rate<0.50"], // Allow up to 50% failures (includes rate limiting)
    errors: ["rate<0.01"], // Custom error rate < 1% (excludes 429)
    product_latency: ["p(95)<200"], // Cached endpoint < 200ms
    user_latency: ["p(95)<200"], // Cached endpoint < 200ms
  },
  summaryTrendStats: ["min", "med", "avg", "p(90)", "p(95)", "p(99)", "max"],
};

// Request headers
const headers = {
  "X-API-Key": API_KEY,
  "Content-Type": "application/json",
};

export default function () {
  // Test 1: GET /api/products (should be heavily cached)
  const productPage = Math.floor(Math.random() * 5);
  const productsRes = http.get(
    `${BASE_URL}/api/products?page=${productPage}&size=20`,
    { headers }
  );

  const productsCheck = check(productsRes, {
    "GET /api/products: status is 200 or 429": (r) =>
      r.status === 200 || r.status === 429,
    "GET /api/products: has content (if 200)": (r) =>
      r.status !== 200 || r.json("content") !== undefined,
  });

  // Only count non-rate-limit errors
  if (productsRes.status !== 429) {
    errorRate.add(!productsCheck);
  }
  productLatency.add(productsRes.timings.duration);

  sleep(1);

  // Test 2: GET /api/users (paginated, cached per page)
  const userPage = Math.floor(Math.random() * 10);
  const usersRes = http.get(`${BASE_URL}/api/users?page=${userPage}&size=20`, {
    headers,
  });

  const usersCheck = check(usersRes, {
    "GET /api/users: status is 200 or 429": (r) =>
      r.status === 200 || r.status === 429,
    "GET /api/users: has content (if 200)": (r) =>
      r.status !== 200 || r.json("content") !== undefined,
  });

  // Only count non-rate-limit errors
  if (usersRes.status !== 429) {
    errorRate.add(!usersCheck);
  }
  userLatency.add(usersRes.timings.duration);

  sleep(1);

  // Test 3: GET /api/orders/{id} (cached, may return 404)
  const orderId = Math.floor(Math.random() * 100) + 1;
  const orderRes = http.get(`${BASE_URL}/api/orders/${orderId}`, { headers });

  const orderCheck = check(orderRes, {
    "GET /api/orders/{id}: status is 200, 404, or 429": (r) =>
      r.status === 200 || r.status === 404 || r.status === 429,
  });

  // Only count non-rate-limit errors
  if (orderRes.status !== 429) {
    errorRate.add(!orderCheck);
  }

  sleep(1);

  // Test 4: POST /api/events (async analytics, should handle high volume)
  const eventTypes = [
    "PAGE_VIEW",
    "BUTTON_CLICK",
    "FORM_SUBMIT",
    "API_CALL",
    "PURCHASE",
  ];
  const eventType = eventTypes[Math.floor(Math.random() * eventTypes.length)];

  const eventPayload = JSON.stringify({
    userId: `user-${Math.floor(Math.random() * 1000)}`,
    eventType: eventType,
    properties: {
      page: "/products",
      referrer: "google",
      timestamp: Date.now(),
    },
  });

  const eventRes = http.post(`${BASE_URL}/api/events`, eventPayload, {
    headers,
  });

  const eventCheck = check(eventRes, {
    "POST /api/events: status is 202 or 429": (r) =>
      r.status === 202 || r.status === 429,
    "POST /api/events: response time < 50ms (if 202)": (r) =>
      r.status !== 202 || r.timings.duration < 50,
  });

  // Only count non-rate-limit errors
  if (eventRes.status !== 429) {
    errorRate.add(!eventCheck);
  }

  sleep(1);
}

// Lifecycle hooks for reporting
export function handleSummary(data) {
  return {
    "load-tests/results/k6-spike/spike-test-summary.json": JSON.stringify(
      data,
      null,
      2
    ),
    "load-tests/results/k6-spike/spike-test-summary.txt": textSummary(data, {
      indent: " ",
      enableColors: false,
    }),
    stdout: textSummary(data, { indent: " ", enableColors: true }),
  };
}

function textSummary(data, opts) {
  const indent = opts.indent || "";
  const enableColors = opts.enableColors !== false;

  let summary = `\n${indent}k6 Spike Test Summary\n${indent}${"=".repeat(
    60
  )}\n\n`;

  // Test duration
  summary += `${indent}Test Duration: ${(
    data.state.testRunDurationMs / 1000
  ).toFixed(1)}s\n`;
  summary += `${indent}Total Requests: ${data.metrics.http_reqs.values.count}\n`;
  summary += `${indent}Request Rate: ${data.metrics.http_reqs.values.rate.toFixed(
    2
  )}/s\n\n`;

  // Response times
  summary += `${indent}Response Times:\n`;
  summary += `${indent}  Min: ${data.metrics.http_req_duration.values.min.toFixed(
    2
  )}ms\n`;
  summary += `${indent}  Med: ${data.metrics.http_req_duration.values.med.toFixed(
    2
  )}ms\n`;
  summary += `${indent}  Avg: ${data.metrics.http_req_duration.values.avg.toFixed(
    2
  )}ms\n`;
  summary += `${indent}  p90: ${data.metrics.http_req_duration.values[
    "p(90)"
  ].toFixed(2)}ms\n`;
  summary += `${indent}  p95: ${data.metrics.http_req_duration.values[
    "p(95)"
  ].toFixed(2)}ms\n`;
  summary += `${indent}  p99: ${data.metrics.http_req_duration.values[
    "p(99)"
  ].toFixed(2)}ms\n`;
  summary += `${indent}  Max: ${data.metrics.http_req_duration.values.max.toFixed(
    2
  )}ms\n\n`;

  // Error rate (includes rate limiting)
  const httpErrorRate = (
    data.metrics.http_req_failed.values.rate * 100
  ).toFixed(2);
  const totalRequests = data.metrics.http_reqs.values.count;
  const failedRequests = Math.round(
    totalRequests * data.metrics.http_req_failed.values.rate
  );
  const successfulRequests = totalRequests - failedRequests;

  summary += `${indent}HTTP Error Rate (includes 429): ${httpErrorRate}%\n`;
  summary += `${indent}Failed Requests: ${failedRequests}\n`;
  summary += `${indent}Successful Requests: ${successfulRequests}\n\n`;

  // Custom error rate (excludes 429)
  const customErrorRate = data.metrics.errors
    ? (data.metrics.errors.values.rate * 100).toFixed(2)
    : "N/A";
  summary += `${indent}Custom Error Rate (excludes 429): ${customErrorRate}%\n`;
  if (data.metrics.errors) {
    summary += `${indent}Real Errors (non-429): ${data.metrics.errors.values.passes}\n`;
  }
  summary += `\n`;

  // Thresholds
  summary += `${indent}Threshold Checks:\n`;
  for (const [metric, threshold] of Object.entries(data.metrics)) {
    if (threshold.thresholds) {
      for (const [name, result] of Object.entries(threshold.thresholds)) {
        const status = result.ok ? "✓" : "✗";
        summary += `${indent}  ${status} ${name}\n`;
      }
    }
  }

  return summary;
}
