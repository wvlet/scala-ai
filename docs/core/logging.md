# Logging

uni provides a comprehensive logging framework with structured logging support.

## LogSupport Trait

The easiest way to add logging is to extend `LogSupport`:

```scala
import wvlet.uni.log.LogSupport

class MyService extends LogSupport:
  def process(data: String): Unit =
    info(s"Processing: ${data}")
    debug("Detailed information")

    try
      riskyOperation()
    catch
      case e: Exception =>
        error("Operation failed", e)
```

## Log Levels

Available log levels in order of severity:

| Level | Method | Use Case |
|-------|--------|----------|
| ERROR | `error()` | Errors requiring attention |
| WARN | `warn()` | Potential issues |
| INFO | `info()` | General information |
| DEBUG | `debug()` | Debugging details |
| TRACE | `trace()` | Fine-grained tracing |

## Logger Configuration

### Setting Log Levels

```scala
import wvlet.uni.log.{Logger, LogLevel}

// Set global default
Logger.setDefaultLogLevel(LogLevel.DEBUG)

// Set for specific logger
Logger("MyService").setLogLevel(LogLevel.TRACE)
```

### Creating Named Loggers

```scala
val logger = Logger("MyApp")
logger.info("Application started")
logger.debug("Configuration loaded")
```

## Logging with Exceptions

Include stack traces in error logs:

```scala
try
  riskyOperation()
catch
  case e: IOException =>
    error("IO operation failed", e)
  case e: Exception =>
    error(s"Unexpected error: ${e.getMessage}", e)
```

## Zero-Overhead Logging with Scala Macros

The logging methods use Scala 3 `inline` macros, which means:

1. **Automatic lazy evaluation**: The message is only evaluated if the log level is enabled
2. **Zero overhead**: If the log level is disabled, there is no runtime cost for creating log messages

```scala
// This is safe and efficient - expensiveComputation() is only called if DEBUG is enabled
debug(s"Result: ${expensiveComputation()}")
```

Unlike traditional logging frameworks, you don't need to wrap expensive computations with level checks:

```scala
// NOT needed - the macro handles this automatically
if logger.isDebugEnabled then
  debug(s"Expensive computation: ${computeDetails()}")

// Just use the log method directly
debug(s"Expensive computation: ${computeDetails()}")
```

## Source Location

Log messages automatically include source location at the end of the message:

```
2024-01-15 10:30:45.123Z  INFO [MyService] Processing: data - (MyService.scala:14)
```

The source code location `(file:line)` is captured at compile time using Scala macros.

## Best Practices

1. **Use appropriate levels** - Don't log everything as INFO
2. **Include context** - Log relevant data for debugging
3. **Avoid sensitive data** - Don't log passwords, tokens, etc.
4. **Use structured data** - Include key-value pairs for parsing
5. **Keep messages concise** - Clear, actionable messages

## Example: Service with Logging

```scala
import wvlet.uni.log.LogSupport

class OrderService(db: Database) extends LogSupport:

  def createOrder(userId: String, items: List[Item]): Order =
    info(s"Creating order for user: ${userId}, items: ${items.size}")

    val order = try
      val order = db.createOrder(userId, items)
      info(s"Order created: ${order.id}")
      order
    catch
      case e: DatabaseException =>
        error(s"Failed to create order for user: ${userId}", e)
        throw e

    debug(s"Order details: ${order}")
    order
```
