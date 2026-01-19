# HTTP Retry Strategies

uni's HTTP client includes automatic retry for transient failures.

## Default Retry Behavior

Retry is enabled by default with sensible defaults:

```scala
val client = Http.client.newSyncClient

// Automatically retries on transient failures
val response = client.send(request)
```

## Configuring Retries

```scala
// Set max retry count
val client = Http.client
  .withConfig(HttpClientConfig().withMaxRetries(5))
  .newSyncClient

// Or per-request
val response = client.withMaxRetry(3).send(request)
```

## Disabling Retry

```scala
// Disable for specific request
val response = client.noRetry.send(request)

// Or globally
val config = HttpClientConfig().withMaxRetries(0)
```

## Retryable Conditions

The HTTP client automatically retries on:

| Condition | Description |
|-----------|-------------|
| Connection errors | Network unreachable, connection refused |
| Timeouts | Connect or read timeout |
| 429 Too Many Requests | Rate limiting |
| 503 Service Unavailable | Temporary outage |
| 502 Bad Gateway | Upstream errors |
| 504 Gateway Timeout | Upstream timeout |

## Non-Retryable Conditions

The following are not retried:

| Condition | Description |
|-----------|-------------|
| 4xx Client errors | 400, 401, 403, 404, etc. (except 429) |
| 5xx Server errors | 500, 501 (except 502, 503, 504) |
| SSL errors | Certificate validation failures |

## Exception Classification

```scala
import wvlet.uni.http.HttpExceptionClassifier

// Check if an exception is retryable
HttpExceptionClassifier.isRetryable(exception) match
  case true => // Will be retried
  case false => // Will not be retried
```

## Backoff Strategy

The HTTP client uses exponential backoff:

1. First retry: ~100ms
2. Second retry: ~200ms
3. Third retry: ~400ms
4. ...with jitter to prevent thundering herd

## Handling Max Retry Exceeded

```scala
import wvlet.uni.http.HttpMaxRetryException

try
  val response = client.send(request)
catch
  case e: HttpMaxRetryException =>
    logger.error(s"Failed after ${e.retryCount} retries")
    logger.error(s"Last error: ${e.lastException.getMessage}")
    // Use fallback or fail
```

## Custom Retry Logic

For more control, use the control module:

```scala
import wvlet.uni.control.Retry

val response = Retry
  .withBackoff(maxRetry = 10)
  .withInitialInterval(500)
  .retryOn {
    case _: IOException => true
    case e: HttpException if e.status.code == 429 => true
    case _ => false
  }
  .run {
    client.noRetry.send(request)
  }
```

## Rate Limiting

Handle rate limits gracefully:

```scala
import wvlet.uni.control.Retry

val response = Retry
  .withBackoff(maxRetry = 5)
  .beforeRetry { (e, retryCount) =>
    e match
      case HttpException(status, _) if status.code == 429 =>
        // Wait longer for rate limits
        Thread.sleep(retryCount * 1000)
      case _ => ()
  }
  .run {
    client.send(request)
  }
```

## Combining with Circuit Breaker

```scala
import wvlet.uni.control.CircuitBreaker

val breaker = CircuitBreaker(maxFailures = 5)

val response = breaker.protect {
  client.send(request)  // Has built-in retry
}
```

## Best Practices

1. **Use default retry** for most cases
2. **Disable for idempotent checks** like auth validation
3. **Monitor retry rates** to detect service issues
4. **Set reasonable limits** to avoid long waits
5. **Log retry attempts** for debugging
