# Rate Limiter Design for Control Module

## Overview

This document outlines the design for a rate limiter component in the `wvlet.uni.control` package. The rate limiter controls the rate of operations over time, complementing existing control mechanisms (CircuitBreaker, Retry, RxSemaphore).

## Background

### Current State

The control module provides:
- **CircuitBreaker**: Fail-fast when service is unhealthy
- **Retry**: Retry failed operations with backoff
- **RxSemaphore**: Limit concurrent operations (permits)
- **RxBoundedQueue**: Backpressure via queue capacity

**Missing**: Rate limiting - controlling how many operations can occur within a time window (e.g., 100 requests/second).

### Use Cases

1. **API Rate Limiting**: Respect external API quotas (e.g., AWS Bedrock, OpenAI)
2. **LLM Request Throttling**: Control token/request rates to avoid throttling
3. **Resource Protection**: Prevent overwhelming downstream services
4. **Cost Control**: Limit usage to control costs

## Requirements

| Requirement | Description |
|-------------|-------------|
| Time-based limiting | Limit N operations per time window |
| Smooth distribution | Avoid request bursts at window boundaries |
| Async-friendly | Integrate with Rx for non-blocking waits |
| Cross-platform | Work on JVM, JS, and Native |
| Composable | Chain with CircuitBreaker and Retry |
| Observable | Expose metrics (available permits, wait time) |

## Design

### Core Interface

```scala
package wvlet.uni.control

/**
  * A rate limiter that controls the rate of operations over time.
  *
  * Unlike RxSemaphore which limits concurrent operations, RateLimiter
  * controls the rate at which operations can be performed (e.g., 100 ops/sec).
  */
trait RateLimiter:

  /**
    * Acquire permission to proceed, blocking if rate limit is exceeded.
    * Returns the wait time in milliseconds (0 if no wait was needed).
    */
  def acquire: Rx[Long]

  /**
    * Acquire n permits, blocking if rate limit is exceeded.
    * Returns the wait time in milliseconds.
    */
  def acquireN(n: Int): Rx[Long]

  /**
    * Try to acquire without waiting.
    * Returns true if permit was acquired, false if rate limit exceeded.
    */
  def tryAcquire: Rx[Boolean]

  /**
    * Try to acquire n permits without waiting.
    */
  def tryAcquireN(n: Int): Rx[Boolean]

  /**
    * Execute an Rx with rate limiting.
    * Automatically waits if rate limit is exceeded.
    */
  def withLimit[A](rx: Rx[A]): Rx[A]

  /**
    * Execute an Rx with rate limiting, using n permits.
    */
  def withLimitN[A](n: Int)(rx: Rx[A]): Rx[A]

  /**
    * Get the current number of available permits.
    */
  def availablePermits: Rx[Double]

  /**
    * Get the estimated wait time in milliseconds for acquiring a permit.
    */
  def estimatedWaitTimeMillis: Rx[Long]

  /**
    * The configured rate (permits per second).
    */
  def ratePerSecond: Double

end RateLimiter
```

### Algorithm: Token Bucket

The Token Bucket algorithm is chosen for its simplicity and smooth rate limiting:

```
┌─────────────────────────────────────────────────────────────────┐
│                      TOKEN BUCKET ALGORITHM                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│    Tokens added at constant rate                                 │
│           │                                                      │
│           ▼                                                      │
│    ┌─────────────┐                                              │
│    │   Bucket    │  capacity = burstSize                        │
│    │  ┌───────┐  │                                              │
│    │  │tokens │  │  tokens ∈ [0, capacity]                      │
│    │  │●●●●●● │  │                                              │
│    │  └───────┘  │                                              │
│    └──────┬──────┘                                              │
│           │                                                      │
│           ▼                                                      │
│    Request consumes tokens                                       │
│    - If tokens >= 1: proceed immediately                         │
│    - If tokens < 1: wait until tokens available                  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**Key Properties:**
- **Smooth limiting**: Tokens refill continuously, not in discrete windows
- **Burst handling**: Bucket capacity allows temporary bursts
- **Simple state**: Only need to track tokens and last refill time
- **Lock-free**: Atomic operations for concurrent access

### Configuration

```scala
case class RateLimiterConfig(
    permitsPerSecond: Double,
    burstSize: Int = 1,
    warmupPeriodMillis: Long = 0L
):
  require(permitsPerSecond > 0, s"permitsPerSecond must be positive: ${permitsPerSecond}")
  require(burstSize >= 1, s"burstSize must be at least 1: ${burstSize}")
  require(warmupPeriodMillis >= 0, s"warmupPeriodMillis must be non-negative: ${warmupPeriodMillis}")

  /** Interval between permits in nanoseconds */
  def intervalNanos: Long = (1_000_000_000.0 / permitsPerSecond).toLong

  def withPermitsPerSecond(rate: Double): RateLimiterConfig =
    this.copy(permitsPerSecond = rate)

  def withBurstSize(size: Int): RateLimiterConfig =
    this.copy(burstSize = size)

  def withWarmupPeriod(millis: Long): RateLimiterConfig =
    this.copy(warmupPeriodMillis = millis)
```

### Implementation

```scala
object RateLimiter:

  /**
    * Create a rate limiter with the given permits per second.
    */
  def apply(permitsPerSecond: Double): RateLimiter =
    new TokenBucketRateLimiter(RateLimiterConfig(permitsPerSecond))

  /**
    * Create a rate limiter from configuration.
    */
  def apply(config: RateLimiterConfig): RateLimiter =
    new TokenBucketRateLimiter(config)

  /**
    * Create a rate limiter with permits per second and burst size.
    */
  def withBurst(permitsPerSecond: Double, burstSize: Int): RateLimiter =
    new TokenBucketRateLimiter(RateLimiterConfig(permitsPerSecond, burstSize))

  /**
    * Create a rate limiter wrapped in Rx.
    */
  def of(permitsPerSecond: Double): Rx[RateLimiter] =
    Rx.single(apply(permitsPerSecond))

  /**
    * A rate limiter that allows all operations (no limiting).
    */
  def unlimited: RateLimiter = UnlimitedRateLimiter

end RateLimiter
```

### Internal Implementation

```scala
private class TokenBucketRateLimiter(config: RateLimiterConfig) extends RateLimiter:

  // State: (storedPermits, nextFreeTicketNanos)
  private case class State(storedPermits: Double, nextFreeTicketNanos: Long)

  private val state = new AtomicReference[State](
    State(config.burstSize.toDouble, System.nanoTime())
  )

  override def ratePerSecond: Double = config.permitsPerSecond

  override def acquire: Rx[Long] = acquireN(1)

  override def acquireN(n: Int): Rx[Long] =
    require(n >= 0, s"Cannot acquire negative permits: ${n}")
    if n == 0 then
      Rx.single(0L)
    else
      Rx.single {
        reserveAndGetWaitTime(n)
      }.flatMap { waitTimeNanos =>
        if waitTimeNanos > 0 then
          val waitMillis = TimeUnit.NANOSECONDS.toMillis(waitTimeNanos)
          Rx.delay(waitMillis, TimeUnit.MILLISECONDS).map(_ => waitMillis)
        else
          Rx.single(0L)
      }

  override def tryAcquire: Rx[Boolean] = tryAcquireN(1)

  override def tryAcquireN(n: Int): Rx[Boolean] =
    require(n >= 0, s"Cannot acquire negative permits: ${n}")
    Rx.single {
      tryReserve(n)
    }

  override def withLimit[A](rx: Rx[A]): Rx[A] =
    acquire.flatMap(_ => rx)

  override def withLimitN[A](n: Int)(rx: Rx[A]): Rx[A] =
    acquireN(n).flatMap(_ => rx)

  override def availablePermits: Rx[Double] =
    Rx.single {
      val current = state.get()
      val nowNanos = System.nanoTime()
      val newPermits = (nowNanos - current.nextFreeTicketNanos).toDouble / config.intervalNanos
      (current.storedPermits + newPermits).min(config.burstSize.toDouble)
    }

  override def estimatedWaitTimeMillis: Rx[Long] =
    Rx.single {
      val current = state.get()
      val nowNanos = System.nanoTime()
      val waitNanos = (current.nextFreeTicketNanos - nowNanos).max(0)
      TimeUnit.NANOSECONDS.toMillis(waitNanos)
    }

  @tailrec
  private def reserveAndGetWaitTime(permits: Int): Long =
    val now = System.nanoTime()
    val current = state.get()

    // Refill tokens based on elapsed time
    val newStoredPermits =
      if now > current.nextFreeTicketNanos then
        val elapsedNanos = now - current.nextFreeTicketNanos
        val newPermits = elapsedNanos.toDouble / config.intervalNanos
        (current.storedPermits + newPermits).min(config.burstSize.toDouble)
      else
        current.storedPermits

    // Calculate wait time
    val storedPermitsToUse = newStoredPermits.min(permits.toDouble)
    val freshPermits = permits - storedPermitsToUse
    val waitNanos = (freshPermits * config.intervalNanos).toLong

    val newNextFreeTicket =
      if now > current.nextFreeTicketNanos then
        now + waitNanos
      else
        current.nextFreeTicketNanos + waitNanos

    val newState = State(
      storedPermits = (newStoredPermits - storedPermitsToUse).max(0),
      nextFreeTicketNanos = newNextFreeTicket
    )

    if state.compareAndSet(current, newState) then
      // Return the wait time (time from now until we can proceed)
      (newNextFreeTicket - now - waitNanos).max(0)
    else
      reserveAndGetWaitTime(permits)

  @tailrec
  private def tryReserve(permits: Int): Boolean =
    val now = System.nanoTime()
    val current = state.get()

    // Can only succeed if nextFreeTicket is in the past
    if current.nextFreeTicketNanos > now then
      false
    else
      // Refill tokens
      val elapsedNanos = now - current.nextFreeTicketNanos
      val newPermits = elapsedNanos.toDouble / config.intervalNanos
      val newStoredPermits = (current.storedPermits + newPermits).min(config.burstSize.toDouble)

      if newStoredPermits < permits then
        false
      else
        val newState = State(
          storedPermits = newStoredPermits - permits,
          nextFreeTicketNanos = now
        )
        if state.compareAndSet(current, newState) then
          true
        else
          tryReserve(permits)

end TokenBucketRateLimiter

private object UnlimitedRateLimiter extends RateLimiter:
  override def acquire: Rx[Long] = Rx.single(0L)
  override def acquireN(n: Int): Rx[Long] = Rx.single(0L)
  override def tryAcquire: Rx[Boolean] = Rx.single(true)
  override def tryAcquireN(n: Int): Rx[Boolean] = Rx.single(true)
  override def withLimit[A](rx: Rx[A]): Rx[A] = rx
  override def withLimitN[A](n: Int)(rx: Rx[A]): Rx[A] = rx
  override def availablePermits: Rx[Double] = Rx.single(Double.MaxValue)
  override def estimatedWaitTimeMillis: Rx[Long] = Rx.single(0L)
  override def ratePerSecond: Double = Double.MaxValue
```

## Alternative Algorithms

### Sliding Window Log

Tracks timestamps of each request in a sliding window:

```
┌─────────────────────────────────────────────────────────────────┐
│                    SLIDING WINDOW LOG                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Time Window (e.g., 1 second)                                    │
│  ←───────────────────────────────────────────────────────────→  │
│                                                                  │
│  │ R1 │ R2 │   │ R3 │ R4 │ R5 │   │ R6 │ R7 │   │              │
│  │    │    │   │    │    │    │   │    │    │   │              │
│  ├────┼────┼───┼────┼────┼────┼───┼────┼────┼───┤──────▶ time  │
│                                                                  │
│  Count requests in window. If count >= limit, reject/wait.       │
│                                                                  │
│  Pros: Precise, no bursts at boundaries                          │
│  Cons: Memory grows with request rate, cleanup needed            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Sliding Window Counter

Approximates sliding window using fixed windows:

```
┌─────────────────────────────────────────────────────────────────┐
│                  SLIDING WINDOW COUNTER                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│    Previous Window        Current Window                         │
│  ┌────────────────┐     ┌────────────────┐                      │
│  │   count = 7    │     │   count = 3    │                      │
│  └────────────────┘     └────────────────┘                      │
│          25%                  75%                                │
│    (time overlap)        (time in window)                        │
│                                                                  │
│  Effective count = 7 * 0.25 + 3 * 0.75 = 4.0                    │
│                                                                  │
│  Pros: Fixed memory, good approximation                          │
│  Cons: Less precise than log, still allows some bursts           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Recommendation

**Token Bucket** is recommended as the primary algorithm because:
1. Simple state (just tokens and timestamp)
2. Smooth rate limiting (continuous token refill)
3. Natural burst handling via bucket capacity
4. Well-understood behavior matching Guava's RateLimiter
5. Lock-free implementation with CAS

Consider adding Sliding Window Counter as an alternative for scenarios requiring stricter rate enforcement without bursts.

## Integration Examples

### With LLM Agent

```scala
import wvlet.uni.control.RateLimiter
import wvlet.uni.agent.{LLMAgent, ChatSession}

// Limit to 10 requests per second for Bedrock
val rateLimiter = RateLimiter(permitsPerSecond = 10.0)

def chat(session: ChatSession, message: String): Rx[ChatResponse] =
  rateLimiter.withLimit {
    session.chat(message)
  }
```

### With CircuitBreaker and Retry

```scala
import wvlet.uni.control.{RateLimiter, CircuitBreaker, Retry}

val rateLimiter = RateLimiter(permitsPerSecond = 100.0)
val circuitBreaker = CircuitBreaker.withConsecutiveFailures(5)
val retry = Retry.withBackOff(maxRetry = 3)

def callExternalApi[A](request: Request): A =
  retry.run {
    circuitBreaker.run {
      rateLimiter.withLimit {
        httpClient.send(request)
      }.run()
    }
  }
```

### Token-Based Rate Limiting

For LLM APIs that rate limit by tokens:

```scala
// Limit to 100,000 tokens per minute
val tokenRateLimiter = RateLimiter.withBurst(
  permitsPerSecond = 100000.0 / 60.0,  // ~1666 tokens/sec
  burstSize = 10000                     // Allow 10K token bursts
)

def chat(request: ChatRequest): Rx[ChatResponse] =
  // Estimate tokens (or use actual count after response)
  val estimatedTokens = estimateTokens(request)
  tokenRateLimiter.withLimitN(estimatedTokens) {
    model.chat(request)
  }
```

## Cross-Platform Considerations

| Feature | JVM | Scala.js | Scala Native |
|---------|-----|----------|--------------|
| Time source | `System.nanoTime()` | `Date.now()` | `System.nanoTime()` |
| AtomicReference | `java.util.concurrent.atomic` | Direct mutation | `java.util.concurrent.atomic` |
| Delay | `Rx.delay` via scheduler | `setTimeout` | Thread sleep |

**Implementation Notes:**
- On JS, use `Date.now() * 1_000_000` for nanosecond approximation
- JS is single-threaded, so CAS loops always succeed on first try
- Native mirrors JVM implementation

## Testing Strategy

### Unit Tests

```scala
class RateLimiterTest extends AirSpec:

  test("acquire at steady rate"):
    val limiter = RateLimiter(permitsPerSecond = 10.0)
    val startTime = System.nanoTime()

    // Acquire 5 permits
    for _ <- 1 to 5 do
      limiter.acquire.run()

    val elapsed = System.nanoTime() - startTime
    // Should take ~400ms for 5 permits at 10/sec
    // (first permit is immediate)
    (elapsed / 1_000_000) shouldBe >= 350L

  test("tryAcquire returns false when rate exceeded"):
    val limiter = RateLimiter(permitsPerSecond = 1.0)
    limiter.tryAcquire.run() shouldBe true
    limiter.tryAcquire.run() shouldBe false

  test("burst allows immediate acquisition up to burst size"):
    val limiter = RateLimiter.withBurst(
      permitsPerSecond = 1.0,
      burstSize = 5
    )

    // Should all succeed immediately
    for _ <- 1 to 5 do
      limiter.tryAcquire.run() shouldBe true

    // 6th should fail
    limiter.tryAcquire.run() shouldBe false

  test("unlimited allows all"):
    val limiter = RateLimiter.unlimited
    for _ <- 1 to 1000 do
      limiter.tryAcquire.run() shouldBe true
```

### Stress Tests

```scala
test("concurrent access"):
  val limiter = RateLimiter(permitsPerSecond = 100.0)
  val counter = new AtomicInteger(0)

  // Launch 10 concurrent threads each trying 100 acquires
  val futures = (1 to 10).map { _ =>
    Future {
      for _ <- 1 to 100 do
        limiter.acquire.run()
        counter.incrementAndGet()
    }
  }

  Await.result(Future.sequence(futures), 30.seconds)
  counter.get() shouldBe 1000
```

## Metrics and Observability

Future extension for metrics:

```scala
trait RateLimiterMetrics:
  /** Total number of successful acquisitions */
  def totalAcquired: Long

  /** Total number of rejected acquisitions (tryAcquire = false) */
  def totalRejected: Long

  /** Total wait time across all acquisitions */
  def totalWaitTimeMillis: Long

  /** Average wait time per acquisition */
  def averageWaitTimeMillis: Double
```

## File Structure

```
uni/src/main/scala/wvlet/uni/control/
├── CircuitBreaker.scala       (existing)
├── Guard.scala                (existing)
├── HealthCheckPolicy.scala    (existing)
├── RateLimiter.scala          (new)
├── Resource.scala             (existing)
├── ResultClass.scala          (existing)
└── Retry.scala                (existing)

uni/src/test/scala/wvlet/uni/control/
├── CircuitBreakerTest.scala   (existing)
├── ControlTest.scala          (existing)
├── RateLimiterTest.scala      (new)
└── RetryTest.scala            (existing)
```

## Summary

The proposed RateLimiter:

1. **Fills a gap** in the control module for time-based rate limiting
2. **Uses Token Bucket** for smooth, predictable rate limiting with burst support
3. **Follows existing patterns** (trait + companion object, `withXXX` configuration)
4. **Integrates with Rx** for non-blocking async waits
5. **Is cross-platform** compatible (JVM/JS/Native)
6. **Composes well** with CircuitBreaker and Retry

This design provides a solid foundation for rate limiting in LLM agent applications and other scenarios requiring controlled request rates.
