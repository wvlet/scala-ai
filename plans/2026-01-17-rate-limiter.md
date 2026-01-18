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
| Multiple algorithms | Support different rate limiting strategies |
| Composable | Chain with CircuitBreaker and Retry |
| Observable | Expose metrics (available permits, wait time) |
| Testable | Ticker abstraction for time-based testing |

## Design

### Core Interface

```scala
trait RateLimiter:
  def acquire(): Long              // Block until permit, return wait time ms
  def acquireN(n: Int): Long       // Acquire N permits
  def tryAcquire(): Boolean        // Non-blocking acquire
  def tryAcquireN(n: Int): Boolean // Non-blocking acquire N
  def withLimit[A](body: => A): A  // Execute with rate limit
  def withLimitN[A](n: Int)(body: => A): A
  def availablePermits: Double
  def estimatedWaitTimeMillis: Long
  def ratePerSecond: Double
```

### Algorithms

Three algorithms are provided:

1. **Token Bucket** (default): Smooth rate limiting with burst support
   - Lock-free using AtomicReference
   - Best for: General use, allows controlled bursts

2. **Fixed Window**: Simple window-based counting
   - Resets at fixed intervals
   - Best for: Simple rate limits, low memory

3. **Sliding Window**: Precise rolling window
   - Tracks timestamps of each request
   - Best for: Strict enforcement, no boundary bursts

### Builder Pattern

```scala
// Token bucket (default)
val limiter = RateLimiter.newBuilder
  .withPermitsPerSecond(10.0)
  .withBurstSize(5)
  .build()

// Fixed window
val fixedWindow = RateLimiter.fixedWindow(
  maxOperations = 100,
  windowDuration = 1,
  unit = TimeUnit.SECONDS
)

// Sliding window
val slidingWindow = RateLimiter.slidingWindow(
  maxOperations = 100,
  windowDuration = 1,
  unit = TimeUnit.SECONDS
)

// Unlimited (no-op)
val unlimited = RateLimiter.unlimited
```

### Usage Examples

```scala
// Simple rate limiting
val limiter = RateLimiter.newBuilder
  .withPermitsPerSecond(10.0)
  .build()

limiter.withLimit {
  callExternalApi()
}

// With CircuitBreaker and Retry
retry.run {
  circuitBreaker.run {
    rateLimiter.withLimit {
      httpClient.send(request)
    }
  }
}

// Token-based rate limiting for LLM APIs
val tokenLimiter = RateLimiter.newBuilder
  .withPermitsPerSecond(100000.0 / 60.0)  // 100K tokens/minute
  .withBurstSize(10000)
  .build()

tokenLimiter.withLimitN(estimatedTokens) {
  model.chat(request)
}
```

## Implementation Notes

- JVM-only due to use of AtomicReference, ReentrantLock, Thread.sleep
- Ticker abstraction enables deterministic testing
- Token bucket is lock-free for high concurrency
- Fixed/sliding window use locks for simpler state management

## Summary

The RateLimiter:
1. Fills a gap in the control module for time-based rate limiting
2. Provides multiple algorithms for different use cases
3. Uses builder pattern for flexible configuration
4. Composes well with CircuitBreaker and Retry
5. Supports testable time via Ticker abstraction
