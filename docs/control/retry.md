# Retry Logic

Handle transient failures by automatically retrying operations.

## Basic Retry

```scala
import wvlet.uni.control.Retry

// Retry up to 3 times on any exception
val result = Retry.withBackoff(maxRetry = 3).run {
  callExternalService()
}
```

## Retry on Specific Exceptions

```scala
// Only retry on specific exception types
val result = Retry.retryOn[NetworkException](maxRetry = 5) {
  makeNetworkCall()
}
```

## Backoff Strategies

### Exponential Backoff

```scala
val result = Retry
  .withBackoff(maxRetry = 5)
  .withInitialInterval(100)  // Start with 100ms
  .withMaxInterval(10000)    // Cap at 10 seconds
  .run {
    operation()
  }
```

### Jitter

Add randomness to prevent thundering herd:

```scala
val result = Retry
  .withBackoff(maxRetry = 5)
  .withJitter(0.1)  // 10% random variation
  .run {
    operation()
  }
```

## Retry Configuration

```scala
val retry = Retry
  .withBackoff(maxRetry = 10)
  .withInitialInterval(100)
  .withMaxInterval(30000)
  .withMultiplier(2.0)
  .withJitter(0.1)
  .retryOn {
    case _: IOException => true
    case _: TimeoutException => true
    case _ => false
  }

val result = retry.run {
  operation()
}
```

## Retry with Result Classification

```scala
import wvlet.uni.control.ResultClass

val result = Retry
  .withBackoff(maxRetry = 3)
  .withResultClassifier {
    case response if response.status == 429 =>
      ResultClass.retryable(new RateLimitException())
    case response if response.status >= 500 =>
      ResultClass.retryable(new ServerException())
    case response =>
      ResultClass.succeeded(response)
  }
  .run {
    httpClient.send(request)
  }
```

## Handling Exhausted Retries

```scala
try
  Retry.withBackoff(maxRetry = 3).run {
    failingOperation()
  }
catch
  case e: MaxRetryException =>
    logger.error(s"Operation failed after ${e.retryCount} retries", e.lastError)
    fallbackBehavior()
```

## Async Retry with Rx

```scala
import wvlet.uni.rx.Rx

Rx.single(asyncOperation())
  .recover {
    case _: NetworkException =>
      // Retry logic can be combined with Rx
      retryOperation()
  }
  .subscribe(handleResult)
```

## Best Practices

1. **Set reasonable limits** - Don't retry forever
2. **Use exponential backoff** - Give services time to recover
3. **Add jitter** - Prevent synchronized retries
4. **Classify errors** - Only retry transient failures
5. **Log retries** - Track retry patterns for debugging
6. **Set timeouts** - Combine with request timeouts

## Common Patterns

### Retry with Fallback

```scala
val result = try
  Retry.withBackoff(maxRetry = 3).run {
    primaryService.call()
  }
catch
  case _: MaxRetryException =>
    fallbackService.call()
```

### Retry with Metrics

```scala
var attempts = 0
val result = Retry
  .withBackoff(maxRetry = 5)
  .run {
    attempts += 1
    metrics.recordAttempt()
    operation()
  }
metrics.recordTotalAttempts(attempts)
```
