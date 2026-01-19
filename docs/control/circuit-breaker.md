# Circuit Breaker

Prevent cascade failures by stopping requests to failing services.

## How It Works

The circuit breaker has three states:

1. **Closed** - Requests flow normally
2. **Open** - Requests are rejected immediately
3. **Half-Open** - Limited requests allowed to test recovery

```
     ┌─────────┐
     │  Closed │ ◄── Normal operation
     └────┬────┘
          │ Failures exceed threshold
          ▼
     ┌─────────┐
     │   Open  │ ◄── Rejecting requests
     └────┬────┘
          │ Reset timeout expires
          ▼
     ┌──────────┐
     │Half-Open │ ◄── Testing recovery
     └────┬─────┘
          │
    Success│ Failure
          │     │
          ▼     ▼
       Closed  Open
```

## Basic Usage

```scala
import wvlet.uni.control.CircuitBreaker
import scala.concurrent.duration.*

val breaker = CircuitBreaker(
  maxFailures = 5,
  callTimeout = 10.seconds,
  resetTimeout = 30.seconds
)

val result = breaker.protect {
  externalServiceCall()
}
```

## Configuration Options

```scala
val breaker = CircuitBreaker(
  maxFailures = 5,          // Open after 5 consecutive failures
  callTimeout = 10.seconds, // Timeout for each call
  resetTimeout = 30.seconds // Time before trying again
)
```

## Handling Open Circuit

```scala
try
  breaker.protect {
    externalService.call()
  }
catch
  case e: CircuitOpenException =>
    // Circuit is open, use fallback
    fallbackResponse()
```

## Circuit State

Check the current state:

```scala
breaker.state match
  case CircuitBreaker.Closed =>
    println("Circuit is healthy")
  case CircuitBreaker.Open =>
    println("Circuit is open, requests blocked")
  case CircuitBreaker.HalfOpen =>
    println("Circuit is testing recovery")
```

## With Retry

Combine circuit breaker with retry:

```scala
import wvlet.uni.control.{CircuitBreaker, Retry}

val breaker = CircuitBreaker(maxFailures = 5)

val result = Retry.withBackoff(maxRetry = 3).run {
  breaker.protect {
    externalService.call()
  }
}
```

## Custom Failure Detection

```scala
val breaker = CircuitBreaker(maxFailures = 5)
  .withFailureDetector {
    case _: NetworkException => true
    case _: TimeoutException => true
    case _ => false  // Don't count as failure
  }
```

## Monitoring

```scala
breaker.onStateChange { (oldState, newState) =>
  logger.info(s"Circuit breaker: ${oldState} -> ${newState}")
  metrics.recordStateChange(newState)
}

breaker.onCallFailure { exception =>
  metrics.recordFailure(exception)
}
```

## Use Cases

### External Service Protection

```scala
class PaymentService(breaker: CircuitBreaker):
  def processPayment(payment: Payment): Result =
    breaker.protect {
      paymentGateway.charge(payment)
    }
```

### Database Protection

```scala
val dbBreaker = CircuitBreaker(
  maxFailures = 3,
  callTimeout = 5.seconds,
  resetTimeout = 10.seconds
)

def queryDatabase(sql: String): Result =
  dbBreaker.protect {
    database.execute(sql)
  }
```

## Best Practices

1. **Tune thresholds** - Based on normal failure rates
2. **Set appropriate timeouts** - Match service SLAs
3. **Monitor state changes** - Alert on frequent opens
4. **Use fallbacks** - Provide degraded functionality
5. **Test failure scenarios** - Verify circuit behavior
