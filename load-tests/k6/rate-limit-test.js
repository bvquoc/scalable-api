/**
 * k6 Rate Limit Validation Test
 *
 * Validates distributed rate limiting enforcement accuracy:
 * - BASIC tier: 60 requests/minute
 * - STANDARD tier: 300 requests/minute
 * - PREMIUM tier: 1000 requests/minute
 *
 * Run with:
 *   k6 run --env BASE_URL=http://localhost:8080 --env API_KEY=test-api-key-local-dev --env TIER=PREMIUM rate-limit-test.js
 *
 * IMPORTANT: API key tier must match TIER parameter!
 * - test-api-key-local-dev has PREMIUM tier (1000 req/min)
 * - For BASIC tier test, use a BASIC tier API key
 *
 * Success Criteria:
 * - Requests 1-{LIMIT} pass (200 OK)
 * - Request {LIMIT+1} fails (429 Too Many Requests)
 * - Retry-After header present in 429 response
 * - Rate limit enforced with 99.9% accuracy
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_KEY = __ENV.API_KEY || 'test-api-key-local-dev';
const TIER = __ENV.TIER || 'BASIC'; // BASIC | STANDARD | PREMIUM

// Rate limit tiers
const RATE_LIMITS = {
  'BASIC': 60,
  'STANDARD': 300,
  'PREMIUM': 1000,
};

const LIMIT = RATE_LIMITS[TIER];
const TEST_REQUESTS = LIMIT + 10; // Test limit + 10 extra to verify rejection

// Custom metrics
const successfulRequests = new Counter('successful_requests');
const rateLimitedRequests = new Counter('rate_limited_requests');
const unexpectedErrors = new Counter('unexpected_errors');

// Single VU executing sequential requests
// Note: Rate limiting is per minute, so we need to send requests rapidly
// within the same minute window to hit the limit
export const options = {
  vus: 1,
  iterations: TEST_REQUESTS,
  maxDuration: '2m',  // Maximum test duration
  thresholds: {
    'successful_requests': [`count>=${LIMIT - 5}`], // Allow 5 requests tolerance
    'rate_limited_requests': ['count>=5'],          // At least 5 rate limited
    'unexpected_errors': ['count<5'],               // < 5 unexpected errors
  },
};

// Request headers
const headers = {
  'X-API-Key': API_KEY,
  'Content-Type': 'application/json',
};

let requestNumber = 0;

export default function () {
  requestNumber++;

  // Simple GET request to test rate limiting
  const res = http.get(
    `${BASE_URL}/api/users?page=0&size=20`,
    { headers }
  );

  const isBeforeLimit = requestNumber <= LIMIT;
  const isAfterLimit = requestNumber > LIMIT;

  // Log every 10th request for debugging
  if (requestNumber % 10 === 0) {
    console.log(`Request ${requestNumber}/${TEST_REQUESTS}: Status ${res.status}`);
  }

  if (isBeforeLimit) {
    // Requests before limit should succeed (200 OK)
    if (res.status === 200) {
      const check200 = check(res, {
        [`Request ${requestNumber}: Status is 200 (before limit)`]: (r) => r.status === 200,
        [`Request ${requestNumber}: Has content`]: (r) => r.json('content') !== undefined,
      });
      
      if (check200) {
        successfulRequests.add(1);
      } else {
        unexpectedErrors.add(1);
      }
    } else if (res.status === 429) {
      // Unexpected: Should not be rate limited before limit
      console.warn(`WARNING: Rate limited at request ${requestNumber}, expected limit: ${LIMIT}`);
      rateLimitedRequests.add(1);
    } else {
      // Other error status
      console.error(`ERROR: Unexpected status ${res.status} at request ${requestNumber}`);
      unexpectedErrors.add(1);
    }

  } else if (isAfterLimit) {
    // Requests after limit should be rate limited
    const check429 = check(res, {
      [`Request ${requestNumber}: Status is 429 (after limit)`]: (r) => r.status === 429,
      [`Request ${requestNumber}: Has Retry-After header`]: (r) => r.headers['Retry-After'] !== undefined,
      [`Request ${requestNumber}: Has error message`]: (r) => r.json('error') !== undefined,
    });

    if (check429) {
      rateLimitedRequests.add(1);
      if (requestNumber === LIMIT + 1) {
        console.log(`✓ Rate limit enforced at request ${requestNumber} (limit: ${LIMIT})`);
        console.log(`  Retry-After: ${res.headers['Retry-After']} seconds`);
      }
    } else if (res.status === 200) {
      console.warn(`WARNING: Request ${requestNumber} succeeded, should be rate limited`);
      successfulRequests.add(1);
    } else {
      console.error(`ERROR: Unexpected status ${res.status} at request ${requestNumber}`);
      unexpectedErrors.add(1);
    }
  }

  // No sleep - rapid fire to hit rate limit within same minute window
  // Rate limiting uses 1-minute windows, so all requests must be in same minute
}

// Summary report
export function handleSummary(data) {
  const results = {
    tier: TIER,
    rateLimit: LIMIT,
    totalRequests: TEST_REQUESTS,
    successfulRequests: data.metrics.successful_requests.values.count || 0,
    rateLimitedRequests: data.metrics.rate_limited_requests.values.count || 0,
    unexpectedErrors: data.metrics.unexpected_errors.values.count || 0,
    accuracy: 0,
    testPassed: false,
  };

  // Calculate accuracy
  const expectedSuccessful = LIMIT;
  const expectedRateLimited = TEST_REQUESTS - LIMIT;
  const actualSuccessful = results.successfulRequests;
  const actualRateLimited = results.rateLimitedRequests;

  const successAccuracy = (actualSuccessful / expectedSuccessful) * 100;
  const rateLimitAccuracy = (actualRateLimited / expectedRateLimited) * 100;
  results.accuracy = Math.min(successAccuracy, rateLimitAccuracy).toFixed(2);

  // Test passes if accuracy > 99%
  results.testPassed = results.accuracy > 99.0;

  // Text summary
  let summary = `\n${'='.repeat(60)}\n`;
  summary += `k6 Rate Limit Validation Test - ${TIER} Tier\n`;
  summary += `${'='.repeat(60)}\n\n`;
  summary += `Rate Limit: ${LIMIT} requests/minute\n`;
  summary += `Total Requests: ${TEST_REQUESTS}\n\n`;
  summary += `Results:\n`;
  summary += `  Successful Requests: ${actualSuccessful} (expected: ${expectedSuccessful})\n`;
  summary += `  Rate Limited Requests: ${actualRateLimited} (expected: ${expectedRateLimited})\n`;
  summary += `  Unexpected Errors: ${results.unexpectedErrors}\n\n`;
  summary += `Accuracy: ${results.accuracy}%\n`;
  summary += `Status: ${results.testPassed ? '✓ PASS' : '✗ FAIL'}\n\n`;

  if (results.testPassed) {
    summary += `✓ Rate limiting enforced correctly for ${TIER} tier\n`;
  } else {
    summary += `✗ Rate limiting accuracy below threshold (99%)\n`;
  }

  summary += `${'='.repeat(60)}\n`;

  return {
    'load-tests/results/k6-rate-limit/rate-limit-test-results.json': JSON.stringify(results, null, 2),
    'load-tests/results/k6-rate-limit/rate-limit-test-summary.txt': summary,
    'stdout': summary,
  };
}
