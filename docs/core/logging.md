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

## Conditional Logging

Avoid expensive computations when log level is disabled:

```scala
if logger.isDebugEnabled then
  debug(s"Expensive computation: ${computeDetails()}")
```

Or use by-name parameters (evaluated lazily):

```scala
debug(s"Result: ${expensiveComputation()}")
// Only computed if DEBUG is enabled
```

## Source Location

Log messages automatically include source location:

```
2024-01-15 10:30:45 INFO  [MyService.scala:25] Processing: data
```

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
