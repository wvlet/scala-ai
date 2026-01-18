# Rx vs Ox Comparison

## Overview

| Aspect | uni (Rx + control) | softwaremill Ox |
|--------|-------------------|-----------------|
| **Platform** | JVM, Scala.js, Scala Native | JVM only |
| **JDK Requirement** | Any JDK | JDK 21+ (Virtual Threads) |
| **Style** | Reactive streams with lazy evaluation | Direct-style with structured concurrency |
| **Backpressure** | Via bounded queues and result signals | Via blocking channel operations |

## Core Abstractions

### uni Rx
- **Rx[A]**: Immutable reactive stream with lazy evaluation
- **RxVar[A]**: Mutable reactive variable with change propagation
- **RxOption[A]**: Optional wrapper over Rx[Option[A]]
- **RxSource[A]**: Push-based data source (RxQueue, RxBoundedQueue)
- **RxFiber[A]**: Lightweight fiber for async computation
- **RxDeferred[A]**: One-time synchronization primitive
- **RxSemaphore**: Permit-based concurrency control
- **RxRef[A]**: Lock-free concurrent reference

### uni control
- **Retry**: Comprehensive retry logic with backoff strategies
- **CircuitBreaker**: 3-state (OPEN/HALF_OPEN/CLOSED) circuit breaker
- **HealthCheckPolicy**: Configurable health check strategies
- **Control**: Resource management (loan pattern)

### Ox
- **supervised { }**: Structured concurrency scope with automatic cleanup
- **fork { }**: Launch concurrent computation within a scope
- **Channel[T]**: Buffered/rendezvous/unlimited channels with select
- **Flow[T]**: Push-based, backpressured streaming pipeline
- **CircuitBreaker**: 3-state circuit breaker
- **RateLimiter**: Rate limiting with multiple algorithms
- **Schedule**: Configurable retry/repeat schedules

## Feature Comparison

### Retry & Backoff

| Feature | uni control | Ox |
|---------|-------------|-----|
| Exponential backoff | Yes (`withBackOff`) | Yes (`Schedule.exponentialBackoff`) |
| Jitter | Yes (`withJitter`) | Yes (`Schedule.jitter`) |
| Bounded backoff | Yes (`withBoundedBackoff`) | Yes (`maxCumulativeDelay`) |
| Max retries | Yes (`withMaxRetry`) | Yes (`maxRetries`) |
| Result classifier | Yes (`withResultClassifier`) | Yes (`ResultPolicy`) |
| Error classifier | Yes (`withErrorClassifier`) | Yes (`isWorthRetrying`) |
| Async retry | Yes (`runAsyncWithContext`) | N/A (direct style) |
| Fibonacci backoff | N/A | Yes (`Schedule.fibonacciBackoff`) |
| Decorrelated jitter | N/A | Yes (`Schedule.decorrelatedJitter`) |

### Circuit Breaker

| Feature | uni control | Ox |
|---------|-------------|-----|
| States | OPEN/HALF_OPEN/CLOSED | OPEN/HALF_OPEN/CLOSED |
| Failure threshold | Yes | Yes (`failureRateThreshold`) |
| Consecutive failures | Yes | N/A |
| Failure rate (EMA) | Yes | Yes |
| Slow call detection | N/A | Yes (`slowCallThreshold`) |
| Recovery policy | Yes (`CircuitBreakerRecoveryPolicy`) | Yes (`numberOfCallsInHalfOpenState`) |
| State change listener | Yes (`onStateChange`) | N/A |
| Fallback handler | Yes (`withFallbackHandler`) | N/A |

### Streaming/Reactive Operations

| Feature | uni Rx | Ox Flow |
|---------|--------|---------|
| map/filter/flatMap | Yes | Yes |
| zip (multiple streams) | Up to 10 | Via operators |
| join (emit on any change) | Up to 10 | N/A |
| merge | Yes | Yes |
| concat | Yes | Yes |
| throttle (first/last) | Yes | N/A |
| sample | Yes | N/A |
| take | Yes | Yes |
| cache with TTL | Yes | N/A |
| interval/timer | Yes | Yes (via tick) |
| grouped/chunked | N/A | Yes |
| mapStateful | N/A | Yes |

### Concurrency Primitives

| Feature | uni Rx | Ox |
|---------|--------|-----|
| Fibers | RxFiber | fork |
| Deferred/Promise | RxDeferred | N/A (direct style) |
| Semaphore | RxSemaphore | N/A (uses permits) |
| Atomic Reference | RxRef | N/A (direct style) |
| Bounded Queue | RxBoundedQueue | Channel (buffered) |
| Select on channels | **N/A** | Yes (Go-like select) |
| Structured scope cleanup | **N/A** | supervised { } |

### Resiliency

| Feature | uni | Ox |
|---------|-----|-----|
| Retry | Yes (control.Retry) | Yes |
| Circuit breaker | Yes (control.CircuitBreaker) | Yes |
| Rate limiter | **N/A (planned)** | Yes (fixed/sliding/leaky bucket) |
| Timeout | Partial (timer + race) | Yes (built-in) |
| Bulkhead | N/A | Yes (via permits) |

## Actual Gaps in uni

### 1. Rate Limiter (High Priority)
Ox provides multiple rate limiting algorithms:
- **Fixed Window**: Max operations per time window
- **Sliding Window**: Rolling window rate limiting
- **Leaky Bucket**: Token-based with periodic replenishment
- Both blocking and drop modes

**Benefit**: Protects downstream services from overload.

### 2. Built-in Timeout Operator (Medium Priority)
Ox has a clean timeout API that:
- Cancels the computation on timeout
- Waits for proper cleanup before returning
- Cleaner than current `timer + race` approach in Rx

### 3. Channel Select (Lower Priority)
Go-like select for waiting on multiple channel operations:
- Non-deterministic selection when multiple channels ready
- Default clause support
- Timeout clause support

### 4. Fibonacci Backoff / Decorrelated Jitter (Low Priority)
Additional backoff strategies:
- Fibonacci: intervals follow Fibonacci sequence
- Decorrelated jitter: random between min and 3*previous

### 5. Slow Call Detection in Circuit Breaker (Low Priority)
Ox's circuit breaker can track slow calls:
- `slowCallThreshold`: percentage of slow calls
- `slowCallDurationThreshold`: what counts as slow

## uni Advantages Over Ox

1. **Cross-Platform**: Works on JVM, Scala.js, and Scala Native (Ox is JVM-only)
2. **JDK Compatibility**: Works on any JDK version (Ox requires JDK 21+)
3. **RxVar**: Mutable reactive variables with change propagation (great for UI)
4. **Rich Operators**: zip/join up to 10 streams, throttle, sample
5. **Cache with TTL**: Built-in caching with expiration
6. **Ticker Abstraction**: Testable time-based operations
7. **Async Retry**: `runAsyncWithContext` integrates retry with Rx
8. **Fallback Handler**: CircuitBreaker has built-in fallback support
9. **State Change Listeners**: CircuitBreaker can notify on state changes
10. **EMA-based Failure Rate**: HealthCheckPolicy uses exponential moving average

## Recommended Actions

### Implement Rate Limiter
Add `RateLimiter` to `wvlet.uni.control` with:
- Fixed window algorithm
- Sliding window algorithm
- Leaky bucket algorithm
- Both blocking and drop modes
- Integration with Rx

### Add Timeout to Rx
Add a cleaner timeout operator:
```scala
def timeout(duration: Long, unit: TimeUnit): Rx[A]
```

### Consider Additional Backoff Strategies
Add to Retry:
- Fibonacci backoff
- Decorrelated jitter

### Consider Slow Call Tracking
Enhance CircuitBreaker with slow call detection.
