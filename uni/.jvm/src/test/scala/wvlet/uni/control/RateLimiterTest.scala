/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.uni.control

import wvlet.uni.test.UniTest
import java.util.concurrent.TimeUnit

class RateLimiterTest extends UniTest:

  // ==================== Token Bucket Tests ====================

  test("token bucket: create with builder") {
    val limiter = RateLimiter.newBuilder.withPermitsPerSecond(10.0).build()

    limiter.ratePerSecond shouldBe 10.0
  }

  test("token bucket: tryAcquire returns true when permits available") {
    val ticker  = Ticker.manualTicker
    val limiter = RateLimiter
      .newBuilder
      .withPermitsPerSecond(10.0)
      .withBurstSize(1)
      .withTicker(ticker)
      .build()

    limiter.tryAcquire() shouldBe true
  }

  test("token bucket: tryAcquire returns false when rate exceeded") {
    val ticker  = Ticker.manualTicker
    val limiter = RateLimiter
      .newBuilder
      .withPermitsPerSecond(1.0)
      .withBurstSize(1)
      .withTicker(ticker)
      .build()

    limiter.tryAcquire() shouldBe true
    limiter.tryAcquire() shouldBe false
  }

  test("token bucket: tryAcquire succeeds after time passes") {
    val ticker  = Ticker.manualTicker
    val limiter = RateLimiter
      .newBuilder
      .withPermitsPerSecond(1.0)
      .withBurstSize(1)
      .withTicker(ticker)
      .build()

    limiter.tryAcquire() shouldBe true
    limiter.tryAcquire() shouldBe false

    // Advance time by 1 second
    ticker.tick(1_000_000_000L)

    limiter.tryAcquire() shouldBe true
  }

  test("token bucket: burst allows immediate acquisition up to burst size") {
    val ticker  = Ticker.manualTicker
    val limiter = RateLimiter
      .newBuilder
      .withPermitsPerSecond(1.0)
      .withBurstSize(5)
      .withTicker(ticker)
      .build()

    for _ <- 1 to 5 do
      limiter.tryAcquire() shouldBe true

    limiter.tryAcquire() shouldBe false
  }

  test("token bucket: tryAcquireN respects permit count") {
    val ticker  = Ticker.manualTicker
    val limiter = RateLimiter
      .newBuilder
      .withPermitsPerSecond(10.0)
      .withBurstSize(5)
      .withTicker(ticker)
      .build()

    limiter.tryAcquireN(3) shouldBe true
    limiter.tryAcquireN(3) shouldBe false
    limiter.tryAcquireN(2) shouldBe true
  }

  test("token bucket: availablePermits refills over time") {
    val ticker  = Ticker.manualTicker
    val limiter = RateLimiter
      .newBuilder
      .withPermitsPerSecond(10.0)
      .withBurstSize(10)
      .withTicker(ticker)
      .build()

    limiter.availablePermits shouldBe 10.0
    limiter.tryAcquireN(10) shouldBe true
    limiter.availablePermits shouldBe 0.0

    // Advance time by 500ms (should refill 5 permits)
    ticker.tick(500_000_000L)
    limiter.availablePermits shouldBe 5.0

    // Advance more time (should cap at burst size)
    ticker.tick(1_000_000_000L)
    limiter.availablePermits shouldBe 10.0
  }

  test("token bucket: withLimit executes body") {
    val limiter = RateLimiter.newBuilder.withPermitsPerSecond(100.0).build()

    var executed = false
    limiter.withLimit {
      executed = true
    }
    executed shouldBe true
  }

  test("token bucket: withLimitN consumes multiple permits") {
    val ticker  = Ticker.manualTicker
    val limiter = RateLimiter
      .newBuilder
      .withPermitsPerSecond(10.0)
      .withBurstSize(5)
      .withTicker(ticker)
      .build()

    var result = 0
    limiter.withLimitN(3) {
      result = 42
    }
    result shouldBe 42

    limiter.tryAcquireN(2) shouldBe true
    limiter.tryAcquire() shouldBe false
  }

  // ==================== Fixed Window Tests ====================

  test("fixed window: basic rate limiting") {
    val ticker  = Ticker.manualTicker
    val limiter = RateLimiter.fixedWindow(3, 100, TimeUnit.MILLISECONDS, ticker)

    limiter.tryAcquire() shouldBe true
    limiter.tryAcquire() shouldBe true
    limiter.tryAcquire() shouldBe true
    limiter.tryAcquire() shouldBe false

    // Advance past window
    ticker.tick(100_000_000L)
    limiter.tryAcquire() shouldBe true
  }

  test("fixed window: tryAcquireN respects permit count") {
    val ticker  = Ticker.manualTicker
    val limiter = RateLimiter.fixedWindow(5, 100, TimeUnit.MILLISECONDS, ticker)

    limiter.tryAcquireN(3) shouldBe true
    limiter.tryAcquireN(3) shouldBe false
    limiter.tryAcquireN(2) shouldBe true
    limiter.tryAcquire() shouldBe false
  }

  test("fixed window: availablePermits") {
    val ticker  = Ticker.manualTicker
    val limiter = RateLimiter.fixedWindow(5, 100, TimeUnit.MILLISECONDS, ticker)

    limiter.availablePermits shouldBe 5.0
    limiter.tryAcquireN(3) shouldBe true
    limiter.availablePermits shouldBe 2.0
  }

  test("fixed window: ratePerSecond calculation") {
    val limiter = RateLimiter.fixedWindow(100, 1, TimeUnit.SECONDS)
    limiter.ratePerSecond shouldBe 100.0
  }

  // ==================== Sliding Window Tests ====================

  test("sliding window: basic rate limiting") {
    val ticker  = Ticker.manualTicker
    val limiter = RateLimiter.slidingWindow(3, 100, TimeUnit.MILLISECONDS, ticker)

    limiter.tryAcquire() shouldBe true
    limiter.tryAcquire() shouldBe true
    limiter.tryAcquire() shouldBe true
    limiter.tryAcquire() shouldBe false

    // Advance past window (need to go slightly past to trigger cleanup)
    ticker.tick(100_000_001L)
    limiter.tryAcquire() shouldBe true
  }

  test("sliding window: tryAcquireN respects permit count") {
    val ticker  = Ticker.manualTicker
    val limiter = RateLimiter.slidingWindow(5, 100, TimeUnit.MILLISECONDS, ticker)

    limiter.tryAcquireN(3) shouldBe true
    limiter.tryAcquireN(3) shouldBe false
    limiter.tryAcquireN(2) shouldBe true
    limiter.tryAcquire() shouldBe false
  }

  test("sliding window: availablePermits") {
    val ticker  = Ticker.manualTicker
    val limiter = RateLimiter.slidingWindow(5, 100, TimeUnit.MILLISECONDS, ticker)

    limiter.availablePermits shouldBe 5.0
    limiter.tryAcquireN(3) shouldBe true
    limiter.availablePermits shouldBe 2.0
  }

  // ==================== Unlimited Tests ====================

  test("unlimited: allows all operations") {
    val limiter = RateLimiter.unlimited

    for _ <- 1 to 1000 do
      limiter.tryAcquire() shouldBe true

    limiter.ratePerSecond shouldBe Double.MaxValue
    limiter.availablePermits shouldBe Double.MaxValue
    limiter.estimatedWaitTimeMillis shouldBe 0L
  }

  // ==================== Builder Validation Tests ====================

  test("builder validates parameters") {
    intercept[IllegalArgumentException] {
      RateLimiter.newBuilder.withPermitsPerSecond(-1.0)
    }

    intercept[IllegalArgumentException] {
      RateLimiter.newBuilder.withBurstSize(0)
    }

    intercept[IllegalArgumentException] {
      RateLimiter.newBuilder.withWarmupPeriod(-100)
    }
  }

  test("acquire validates permits") {
    val limiter = RateLimiter.newBuilder.withPermitsPerSecond(10.0).build()

    intercept[IllegalArgumentException] {
      limiter.acquireN(-1)
    }

    intercept[IllegalArgumentException] {
      limiter.tryAcquireN(-1)
    }
  }

  test("acquireN with 0 permits succeeds immediately") {
    val limiter = RateLimiter.newBuilder.withPermitsPerSecond(1.0).build()

    limiter.acquireN(0) shouldBe 0L
    limiter.tryAcquireN(0) shouldBe true
  }

  test("builder with name") {
    val limiter = RateLimiter.newBuilder.withPermitsPerSecond(10.0).withName("test-limiter").build()

    limiter.ratePerSecond shouldBe 10.0
  }

  test("builder noName removes name") {
    val builder = RateLimiter.newBuilder.withPermitsPerSecond(10.0).withName("test").noName()

    builder.name shouldBe None
  }

end RateLimiterTest
