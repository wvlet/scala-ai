package wvlet.uni.control

import wvlet.uni.test.*

class RateLimiterTest extends UniTest:

  test("create rate limiter with builder") {
    val limiter = RateLimiter.newBuilder
      .withPermitsPerSecond(10.0)
      .build()

    limiter.ratePerSecond shouldBe 10.0
  }

  test("create rate limiter with burst size") {
    val limiter = RateLimiter.newBuilder
      .withPermitsPerSecond(1.0)
      .withBurstSize(5)
      .build()

    limiter.ratePerSecond shouldBe 1.0
  }

  test("tryAcquire returns true when permits available") {
    val ticker = Ticker.manualTicker
    val limiter = RateLimiter.newBuilder
      .withPermitsPerSecond(10.0)
      .withBurstSize(1)
      .withTicker(ticker)
      .build()

    limiter.tryAcquire() shouldBe true
  }

  test("tryAcquire returns false when rate exceeded") {
    val ticker = Ticker.manualTicker
    val limiter = RateLimiter.newBuilder
      .withPermitsPerSecond(1.0)
      .withBurstSize(1)
      .withTicker(ticker)
      .build()

    limiter.tryAcquire() shouldBe true
    limiter.tryAcquire() shouldBe false
  }

  test("tryAcquire succeeds after time passes") {
    val ticker = Ticker.manualTicker
    val limiter = RateLimiter.newBuilder
      .withPermitsPerSecond(1.0)
      .withBurstSize(1)
      .withTicker(ticker)
      .build()

    limiter.tryAcquire() shouldBe true
    limiter.tryAcquire() shouldBe false

    // Advance time by 1 second (1 billion nanos)
    ticker.tick(1_000_000_000L)

    limiter.tryAcquire() shouldBe true
  }

  test("burst allows immediate acquisition up to burst size") {
    val ticker = Ticker.manualTicker
    val limiter = RateLimiter.newBuilder
      .withPermitsPerSecond(1.0)
      .withBurstSize(5)
      .withTicker(ticker)
      .build()

    // Should all succeed immediately due to burst
    for i <- 1 to 5 do
      limiter.tryAcquire() shouldBe true

    // 6th should fail
    limiter.tryAcquire() shouldBe false
  }

  test("tryAcquireN respects permit count") {
    val ticker = Ticker.manualTicker
    val limiter = RateLimiter.newBuilder
      .withPermitsPerSecond(10.0)
      .withBurstSize(5)
      .withTicker(ticker)
      .build()

    limiter.tryAcquireN(3) shouldBe true
    limiter.tryAcquireN(3) shouldBe false
    limiter.tryAcquireN(2) shouldBe true
  }

  test("unlimited allows all operations") {
    val limiter = RateLimiter.unlimited

    for _ <- 1 to 1000 do
      limiter.tryAcquire() shouldBe true

    limiter.ratePerSecond shouldBe Double.MaxValue
    limiter.availablePermits shouldBe Double.MaxValue
    limiter.estimatedWaitTimeMillis shouldBe 0L
  }

  test("availablePermits refills over time") {
    val ticker = Ticker.manualTicker
    val limiter = RateLimiter.newBuilder
      .withPermitsPerSecond(10.0)  // 1 permit per 100ms
      .withBurstSize(10)
      .withTicker(ticker)
      .build()

    // Initial permits = burst size
    limiter.availablePermits shouldBe 10.0

    // Use all permits
    limiter.tryAcquireN(10) shouldBe true
    limiter.availablePermits shouldBe 0.0

    // Advance time by 500ms (should refill 5 permits at 10/sec rate)
    ticker.tick(500_000_000L)
    limiter.availablePermits shouldBe 5.0

    // Advance time to 1 second total (should cap at burst size)
    ticker.tick(1_000_000_000L)
    limiter.availablePermits shouldBe 10.0  // capped at burst size
  }

  test("withLimit executes body") {
    val limiter = RateLimiter.newBuilder
      .withPermitsPerSecond(100.0)
      .build()

    var executed = false
    limiter.withLimit {
      executed = true
    }
    executed shouldBe true
  }

  test("withLimitN consumes multiple permits") {
    val ticker = Ticker.manualTicker
    val limiter = RateLimiter.newBuilder
      .withPermitsPerSecond(10.0)
      .withBurstSize(5)
      .withTicker(ticker)
      .build()

    var result = 0
    limiter.withLimitN(3) {
      result = 42
    }
    result shouldBe 42

    // Should have consumed 3 permits, leaving 2
    limiter.tryAcquireN(2) shouldBe true
    limiter.tryAcquire() shouldBe false
  }

  test("estimatedWaitTimeMillis returns wait time") {
    val ticker = Ticker.manualTicker
    val limiter = RateLimiter.newBuilder
      .withPermitsPerSecond(1.0)  // 1 permit per second
      .withBurstSize(1)
      .withTicker(ticker)
      .build()

    // Initially no wait
    limiter.estimatedWaitTimeMillis shouldBe 0L

    // Acquire the permit
    limiter.tryAcquire() shouldBe true

    // No wait time since we already acquired and nextFreeTicket is in the past
    limiter.estimatedWaitTimeMillis shouldBe 0L
  }

  test("acquire returns wait time") {
    val ticker = Ticker.manualTicker
    val limiter = RateLimiter.newBuilder
      .withPermitsPerSecond(1000.0)  // Fast rate to minimize actual sleep
      .withBurstSize(1)
      .withTicker(ticker)
      .build()

    // First acquire should be immediate
    val wait1 = limiter.acquire()
    wait1 shouldBe 0L
  }

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
    val limiter = RateLimiter.newBuilder
      .withPermitsPerSecond(10.0)
      .build()

    intercept[IllegalArgumentException] {
      limiter.acquireN(-1)
    }

    intercept[IllegalArgumentException] {
      limiter.tryAcquireN(-1)
    }
  }

  test("acquireN with 0 permits succeeds immediately") {
    val limiter = RateLimiter.newBuilder
      .withPermitsPerSecond(1.0)
      .build()

    limiter.acquireN(0) shouldBe 0L
    limiter.tryAcquireN(0) shouldBe true
  }

  test("builder with name") {
    val limiter = RateLimiter.newBuilder
      .withPermitsPerSecond(10.0)
      .withName("test-limiter")
      .build()

    limiter.ratePerSecond shouldBe 10.0
  }

  test("builder noName removes name") {
    val builder = RateLimiter.newBuilder
      .withPermitsPerSecond(10.0)
      .withName("test")
      .noName()

    builder.name shouldBe None
  }

end RateLimiterTest
