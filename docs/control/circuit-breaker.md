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

## Factory Methods

### Default Circuit Breaker

```scala
import wvlet.uni.control.CircuitBreaker

val cb = CircuitBreaker.default

val result = cb.run {
  externalServiceCall()
}
```

### Failure Threshold

Open circuit when failures exceed threshold within a window:

```scala
// Open when 2 out of 5 recent calls fail
val cb = CircuitBreaker.withFailureThreshold(2, 5)

val result = cb.run {
  externalServiceCall()
}
```

### Consecutive Failures

Open circuit after consecutive failures:

```scala
// Open after 2 consecutive failures
val cb = CircuitBreaker.withConsecutiveFailures(2)

val result = cb.run {
  externalServiceCall()
}
```

### Always Closed (Disabled)

Disable circuit breaker for testing:

```scala
val cb = CircuitBreaker.alwaysClosed

// Never opens, even with many failures
for i <- 0 to 10 do
  cb.recordFailure(new Exception())
  cb.isConnected shouldBe true
```

## Running Code with Circuit Breaker

Use `run` to execute code through the circuit breaker:

```scala
val cb = CircuitBreaker.withFailureThreshold(1, 2)

// Successful execution
cb.run {
  operation()
}

// If the operation throws, it's recorded as a failure
try
  cb.run[Any] {
    throw new TimeoutException()
  }
catch
  case e: TimeoutException => // Handle exception
```

## Handling Open Circuit

```scala
import wvlet.uni.control.{CircuitBreaker, CircuitBreakerOpenException}

val cb = CircuitBreaker.withFailureThreshold(1, 2)

try
  cb.run {
    externalService.call()
  }
catch
  case e: CircuitBreakerOpenException =>
    // Circuit is open, use fallback
    fallbackResponse()
```

## Circuit State

Check and control the current state:

```scala
val cb = CircuitBreaker.default

// Check state
cb.state match
  case CircuitBreaker.CLOSED =>
    println("Circuit is healthy")
  case CircuitBreaker.OPEN =>
    println("Circuit is open, requests blocked")
  case CircuitBreaker.HALF_OPEN =>
    println("Circuit is testing recovery")

// Check if connected
if cb.isConnected then
  println("Circuit breaker allows connections")
```

### Manual State Control

```scala
val cb = CircuitBreaker.default

cb.state shouldBe CircuitBreaker.CLOSED
cb.isConnected shouldBe true

// Manually open the circuit
cb.open()
cb.state shouldBe CircuitBreaker.OPEN
cb.isConnected shouldBe false

// Set to half-open for probing
cb.halfOpen()
cb.state shouldBe CircuitBreaker.HALF_OPEN
cb.isConnected shouldBe true

// Close the circuit
cb.close()
cb.state shouldBe CircuitBreaker.CLOSED
cb.isConnected shouldBe true
```

## Standalone Usage

Record successes and failures manually:

```scala
val cb = CircuitBreaker.default

// Verify connection before making request
cb.verifyConnection()

try
  val result = externalService.call()
  cb.recordSuccess()
  result
catch
  case e: Throwable =>
    cb.recordFailure(e)
    throw e
```

## With Retry

Combine circuit breaker with retry:

```scala
import wvlet.uni.control.{CircuitBreaker, Retry}

val cb = CircuitBreaker.withFailureThreshold(5, 10)

val result = Retry.withBackOff(maxRetry = 3).run {
  cb.run {
    externalService.call()
  }
}
```

## Use Cases

### External Service Protection

```scala
class PaymentService(cb: CircuitBreaker):
  def processPayment(payment: Payment): Result =
    cb.run {
      paymentGateway.charge(payment)
    }
```

### Database Protection

```scala
val dbBreaker = CircuitBreaker.withConsecutiveFailures(3)

def queryDatabase(sql: String): Result =
  dbBreaker.run {
    database.execute(sql)
  }
```

### API Gateway

```scala
val apiBreaker = CircuitBreaker.withFailureThreshold(2, 5)

def callApi(request: Request): Response =
  try
    apiBreaker.run {
      httpClient.send(request)
    }
  catch
    case _: CircuitBreakerOpenException =>
      Response.serviceUnavailable()
```

## Best Practices

1. **Tune thresholds** - Based on normal failure rates
2. **Use failure threshold** - For services with variable error rates
3. **Use consecutive failures** - For services where any failure is significant
4. **Monitor state changes** - Alert on frequent opens
5. **Use fallbacks** - Provide degraded functionality
6. **Test failure scenarios** - Verify circuit behavior
