# Retry Logic

Handle transient failures by automatically retrying operations.

## Basic Retry

```scala
import wvlet.uni.control.Retry

// Retry up to 3 times with exponential backoff
val result = Retry.withBackOff(maxRetry = 3).run {
  callExternalService()
}
```

## Factory Methods

### Exponential Backoff

```scala
// Default exponential backoff
val result = Retry.withBackOff(maxRetry = 5).run {
  operation()
}

// With custom initial interval
val result = Retry
  .withBackOff(initialIntervalMillis = 100)
  .withMaxRetry(5)
  .run {
    operation()
  }
```

### Jitter

Add randomness to prevent thundering herd:

```scala
val result = Retry.withJitter(maxRetry = 3).run {
  operation()
}
```

### Bounded Backoff

Limit total wait time:

```scala
val retry = Retry.withBoundedBackoff(
  initialIntervalMillis = 1000,
  maxTotalWaitMillis = 50000
)

val result = retry.run {
  operation()
}
```

## Retry on Specific Conditions

Use `retryOn` with a partial function returning `ResultClass`:

```scala
import wvlet.uni.control.{Retry, ResultClass}

val result = Retry
  .withBackOff(maxRetry = 3)
  .retryOn { case e: IllegalStateException =>
    warn(e.getMessage)
    ResultClass.retryableFailure(e)
  }
  .run {
    operation()
  }
```

## Error Classification

Use `withErrorClassifier` for custom error handling:

```scala
import wvlet.uni.control.{Retry, ResultClass}
import scala.concurrent.TimeoutException

val result = Retry
  .withBackOff(initialIntervalMillis = 10)
  .withMaxRetry(3)
  .withErrorClassifier { case e: TimeoutException =>
    ResultClass.retryableFailure(e).withExtraWaitMillis(100)
  }
  .run {
    operation()
  }
```

### Extra Wait Options

```scala
// Add fixed extra wait time
ResultClass.retryableFailure(e).withExtraWaitMillis(100)

// Add proportional extra wait time (20% of base interval)
ResultClass.retryableFailure(e).withExtraWaitFactor(0.2)
```

## Retry Configuration

```scala
val retry = Retry
  .withBackOff(initialIntervalMillis = 100)
  .withMaxRetry(10)
  .noRetryLogging  // Disable retry logging

val result = retry.run {
  operation()
}
```

### Switching Strategies

```scala
// Start with jitter, switch to backoff
val r = Retry.withJitter().withBackOff(initialIntervalMillis = 3)

// Switch back to jitter
val j = r.withJitter(initialIntervalMillis = 20)

// Change max retry
val m = j.withMaxRetry(100)
```

## Before Retry Hook

Execute code before each retry:

```scala
import wvlet.uni.control.Retry.RetryContext

val result = Retry
  .withBackOff(initialIntervalMillis = 0)
  .beforeRetry { (ctx: RetryContext) =>
    logger.info(s"Retry ${ctx.retryCount}, next wait: ${ctx.nextWaitMillis}ms")
  }
  .run {
    operation()
  }
```

## Run with Context

Pass context through retry attempts:

```scala
import wvlet.uni.control.Retry.RetryContext

val result = Retry
  .withBackOff(initialIntervalMillis = 0)
  .beforeRetry { (ctx: RetryContext) =>
    // Access the context
    ctx.context shouldBe Some("hello world")
  }
  .runWithContext("hello world") {
    operation()
  }
```

## Handling Exhausted Retries

```scala
import wvlet.uni.control.Retry.MaxRetryException

try
  Retry.withBackOff(maxRetry = 3).run {
    failingOperation()
  }
catch
  case e: MaxRetryException =>
    logger.error(s"Failed after ${e.retryContext.retryCount} retries")
    logger.error(s"Last error: ${e.retryContext.lastError}")
    fallbackBehavior()
```

## Best Practices

1. **Set reasonable limits** - Don't retry forever
2. **Use exponential backoff** - Give services time to recover
3. **Add jitter** - Prevent synchronized retries with `withJitter`
4. **Classify errors** - Only retry transient failures using `retryOn`
5. **Log retries** - Use `beforeRetry` for debugging
6. **Set timeouts** - Combine with request timeouts

## Common Patterns

### Retry with Fallback

```scala
import wvlet.uni.control.Retry.MaxRetryException

val result = try
  Retry.withBackOff(maxRetry = 3).run {
    primaryService.call()
  }
catch
  case _: MaxRetryException =>
    fallbackService.call()
```

### Retry with Metrics

```scala
import wvlet.uni.control.Retry.RetryContext

var attempts = 0
val result = Retry
  .withBackOff(maxRetry = 5)
  .beforeRetry { (ctx: RetryContext) =>
    metrics.recordRetry(ctx.retryCount)
  }
  .run {
    attempts += 1
    operation()
  }
metrics.recordTotalAttempts(attempts)
```
