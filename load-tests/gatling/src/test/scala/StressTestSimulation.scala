package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Gatling Stress Test: Gradual ramp from 100 → 1000 users to find breaking point
 *
 * Run with: mvn gatling:test -Dgatling.simulationClass=simulations.StressTestSimulation
 *
 * Performance Targets:
 * - p95 response time: <200ms (normal load), <500ms (stress)
 * - Success rate: >99%
 * - Breaking point: Load level where error rate >1%
 */
class StressTestSimulation extends Simulation {

  // Load configuration from system properties or use defaults
  val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")
  val apiKey = System.getProperty("apiKey", "test-api-key-local-dev")

  // HTTP Protocol Configuration
  val httpProtocol = http
    .baseUrl(baseUrl)
    .header("X-API-Key", apiKey)
    .header("Accept", "application/json")
    .header("Content-Type", "application/json")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Gatling Stress Test / 1.0")
    .shareConnections // Connection pooling

  // Feeders for dynamic data
  val userPageFeeder = Iterator.continually(Map("userPage" -> scala.util.Random.nextInt(10)))
  val productPageFeeder = Iterator.continually(Map("productPage" -> scala.util.Random.nextInt(5)))
  val orderIdFeeder = Iterator.continually(Map("orderId" -> (scala.util.Random.nextInt(100) + 1)))

  // Scenario: Read-Heavy Workload (GET requests)
  val readHeavyScenario = scenario("Read-Heavy Workload")
    .exec(
      feed(userPageFeeder),
      http("GET /api/users")
        .get("/api/users?page=#{userPage}&size=20")
        .check(status.in(200))
        .check(jsonPath("$.content").exists)
    )
    .pause(1.second, 2.seconds)
    .exec(
      feed(productPageFeeder),
      http("GET /api/products")
        .get("/api/products?page=#{productPage}&size=20")
        .check(status.in(200))
        .check(jsonPath("$.content").exists)
    )
    .pause(1.second, 2.seconds)
    .exec(
      feed(orderIdFeeder),
      http("GET /api/orders/{id}")
        .get("/api/orders/#{orderId}")
        .check(status.in(200, 404)) // Accept 404 if order doesn't exist
    )
    .pause(1.second, 3.seconds)

  // Scenario: Mixed Read/Write Workload
  val mixedWorkloadScenario = scenario("Mixed Read/Write Workload")
    .exec(
      http("POST /api/events")
        .post("/api/events")
        .body(StringBody("""{
          "userId": "user-#{__Random(1,1000)}",
          "eventType": "PAGE_VIEW",
          "properties": {
            "page": "/products",
            "referrer": "google"
          }
        }""")).asJson
        .check(status.is(202)) // Analytics events return 202 Accepted
    )
    .pause(500.milliseconds, 1.second)
    .exec(
      feed(productPageFeeder),
      http("GET /api/products")
        .get("/api/products?page=#{productPage}&size=20")
        .check(status.in(200))
    )
    .pause(1.second, 2.seconds)

  // Load Profile: Gradual stress ramp
  // Using single scenario with rampUsersPerSec to avoid duplicate scenario names
  setUp(
    readHeavyScenario.inject(
      // Phase 1: Warm-up (0 → 100 users over 1 min)
      rampUsersPerSec(0).to(100).during(1.minute),
      // Phase 2: Normal load (100 → 200 users over 1 min)
      rampUsersPerSec(100).to(200).during(1.minute),
      // Phase 3: Increased load (200 → 400 users over 1 min)
      rampUsersPerSec(200).to(400).during(1.minute),
      // Phase 4: Heavy load (400 → 600 users over 1 min)
      rampUsersPerSec(400).to(600).during(1.minute),
      // Phase 5: Stress load (600 → 800 users over 1 min)
      rampUsersPerSec(600).to(800).during(1.minute),
      // Phase 6: Breaking point (800 → 1000 users over 1 min)
      rampUsersPerSec(800).to(1000).during(1.minute),
      // Hold at peak for 1 minute
      constantUsersPerSec(1000).during(1.minute)
    ).protocols(httpProtocol),
    
    // Mixed workload (lower concurrency for write operations)
    mixedWorkloadScenario.inject(
      rampUsers(100).during(7.minutes)
    ).protocols(httpProtocol)
  ).assertions(
    // Performance assertions
    global.responseTime.percentile3.lt(500), // p95 < 500ms
    global.successfulRequests.percent.gt(99), // >99% success rate
    details("GET /api/users").responseTime.percentile3.lt(200), // p95 < 200ms for cached endpoints
    details("GET /api/products").responseTime.percentile3.lt(200)
  )
}
