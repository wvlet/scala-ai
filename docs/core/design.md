# Design

Design is a framework for building and wiring complex objects at runtime. It provides a declarative way to define how objects are constructed and connected, managing their lifecycles automatically.

## Core Concepts

Design is built around two fundamental abstractions:

- **Design**: An immutable configuration that maps types to their implementations or instances
- **Session**: A runtime context that manages singleton instances and object lifecycles

The framework emphasizes constructor-based wiring - dependencies are provided through class constructors, keeping your application code decoupled from the framework itself.

## Quick Start

### Step 1: Define Classes with Dependencies

```scala
import wvlet.uni.design.Design

case class AppConfig(appName: String)

class MyApp(val config: AppConfig):
  def greet(): String = s"Hello from ${config.appName}!"
```

### Step 2: Create a Design

```scala
val design = Design.newDesign
  .bindInstance[AppConfig](AppConfig("My Application"))
```

### Step 3: Build and Use

```scala
design.build[MyApp] { app =>
  println(app.greet())  // Prints: Hello from My Application!
}
// Session automatically starts and shuts down
```

## Basic Usage

### Creating a Design

```scala
import wvlet.uni.design.Design

// Define interfaces
trait DatabaseService:
  def findUser(id: String): Option[User]

trait UserService:
  def getUser(id: String): User

// Define implementations
class PostgresDatabase extends DatabaseService:
  def findUser(id: String): Option[User] = ???

class UserServiceImpl(db: DatabaseService) extends UserService:
  def getUser(id: String): User =
    db.findUser(id).getOrElse(throw RuntimeException("Not found"))

// Create a design
val design = Design.newDesign
  .bindImpl[DatabaseService, PostgresDatabase]
  .bindImpl[UserService, UserServiceImpl]
```

### Building Instances

```scala
// Build and use a service
design.build[UserService] { userService =>
  val user = userService.getUser("123")
  println(s"Found: ${user}")
}
```

## Binding Types

### Implementation Binding

Bind an interface to its implementation class:

```scala
design.bindImpl[Service, ServiceImpl]
```

This creates a singleton instance of `ServiceImpl` whenever `Service` is requested.

### Instance Binding

Bind a pre-created instance directly:

```scala
val config = Config("localhost", 8080)
design.bindInstance[Config](config)
```

### Singleton Binding

Ensure only one instance is created and shared:

```scala
design.bindSingleton[DatabasePool]
```

### Provider Binding

Use a factory function to create instances with dependencies:

```scala
// Single dependency
design.bindProvider[Config, HttpClient] { config =>
  HttpClient(config.host, config.port)
}

// Multiple dependencies
design.bindProvider[Config, Logger, HttpClient] { (config, logger) =>
  logger.info(s"Creating client for ${config.host}")
  HttpClient(config.host, config.port)
}
```

### Eager Singleton Binding

Initialize singletons immediately when the session starts, rather than lazily:

```scala
design.bindEagerSingleton[CacheService]
```

## Lifecycle Management

Design provides comprehensive lifecycle management for your objects.

### Lifecycle Hooks

```scala
val design = Design.newDesign
  .bindSingleton[DatabasePool]
  .onInit { pool =>
    // Called when the object is first created
    pool.initialize()
  }
  .onStart { pool =>
    // Called when the session starts
    pool.connect()
  }
  .onShutdown { pool =>
    // Called when the session shuts down
    pool.close()
  }
```

### Complete Lifecycle Order

1. `onInit`: Object initialization (called once when created)
2. `onInject`: After object is injected
3. `onStart`: When session starts
4. `afterStart`: After all onStart hooks complete
5. `beforeShutdown`: Before shutdown begins
6. `onShutdown`: Session shutdown (cleanup)

### AutoCloseable Support

Classes implementing `AutoCloseable` are automatically closed on session shutdown:

```scala
class MyDB(name: String) extends AutoCloseable:
  private val conn = newConnection(name)
  override def close(): Unit = conn.close()

// MyDB.close() called automatically when session ends
design.bindSingleton[MyDB]
```

## Session Management

Sessions hold singleton instances and manage their lifecycles.

### Automatic Session Management

The recommended approach - session lifecycle is handled automatically:

```scala
design.build[App] { app =>
  // Session started, app is ready to use
  app.run()
}
// Session automatically shuts down here
```

### Manual Session Control

For more control over the session lifecycle:

```scala
val session = design.newSession
try
  session.start()
  val app = session.build[App]
  app.run()
finally
  session.shutdown()
```

### Type-Safe Return Values

Use `run` to return values from the session scope:

```scala
val result: Int = design.run { (service: ComputeService) =>
  service.calculate()
}
```

### Accessing the Session

Inject the session itself to register dynamically created resources:

```scala
import wvlet.uni.design.Session

class MyApp(session: Session):
  def openDB(name: String): MyDB =
    val db = MyDB(name)
    session.register(db)  // Register for lifecycle management
    db
```

### Child Sessions

Create child sessions for request-scoped lifecycles:

```scala
import wvlet.uni.design.Session

class MyServer(session: Session):
  def handleRequest() =
    val requestDesign = Design.newDesign
      .bindSingleton[RequestContext]

    session.withChildSession(requestDesign) { childSession =>
      val ctx = childSession.build[RequestContext]
      // RequestContext is scoped to this request
      processRequest(ctx)
    }
    // RequestContext is cleaned up here
```

## Design Composition

Designs are immutable and can be combined:

```scala
val loggingDesign = Design.newDesign
  .bindImpl[Logger, ConsoleLogger]

val databaseDesign = Design.newDesign
  .bindImpl[DatabaseService, PostgresDatabase]

val serviceDesign = Design.newDesign
  .bindImpl[UserService, UserServiceImpl]

// Combine all designs
val fullDesign = loggingDesign + databaseDesign + serviceDesign
```

Later bindings override earlier ones:

```scala
val baseDesign = Design.newDesign
  .bindImpl[Cache, InMemoryCache]

val redisDesign = Design.newDesign
  .bindImpl[Cache, RedisCache]

// RedisCache will be used for Cache
val combined = baseDesign + redisDesign
```

## Production Mode

In production mode, all singletons are initialized eagerly at session start:

```scala
design
  .withProductionMode
  .build[App] { app =>
    // All singletons already initialized
    app.run()
  }
```

This helps catch configuration errors early and ensures predictable startup behavior.

## Generic Type Binding

Design distinguishes between different generic type parameters:

```scala
// These are distinct bindings
design
  .bindInstance[Seq[Int]](Seq(1, 2, 3))
  .bindInstance[Seq[String]](Seq("a", "b", "c"))
  .bindInstance[Map[String, Int]](Map("x" -> 1))
```

## Tagged Type Binding

Use tagged types to bind multiple instances of the same type:

```scala
import wvlet.uni.design.tag.*

// Define tags as traits
trait Production
trait Staging

case class DatabaseConfig(url: String)

// Service that needs both configs
class MigrationService(
  prodDb: DatabaseConfig @@ Production,
  stagingDb: DatabaseConfig @@ Staging
):
  def migrate(): Unit =
    println(s"Migrating from ${stagingDb.url} to ${prodDb.url}")

// Bind different configs with tags
val design = Design.newDesign
  .bindInstance[DatabaseConfig @@ Production](DatabaseConfig("jdbc:prod://..."))
  .bindInstance[DatabaseConfig @@ Staging](DatabaseConfig("jdbc:staging://..."))

design.build[MigrationService] { service =>
  service.migrate()
}
```

### Primitive Type Tagging

Useful for configuration values:

```scala
import wvlet.uni.design.tag.*

trait Port
trait Host

class ServerConfig(host: String @@ Host, port: Int @@ Port):
  def address: String = s"${host}:${port}"

val design = Design.newDesign
  .bindInstance[String @@ Host]("localhost")
  .bindInstance[Int @@ Port](8080)
```

## Testing

Design makes testing straightforward by allowing you to override bindings:

```scala
// Production design
val prodDesign = Design.newDesign
  .bindImpl[EmailService, SmtpEmailService]
  .bindImpl[UserRepository, PostgresUserRepository]

// Test design - override specific bindings
val testDesign = prodDesign
  .bindImpl[EmailService, MockEmailService]
  .bindImpl[UserRepository, InMemoryUserRepository]

// Use in tests
testDesign.build[UserService] { service =>
  val result = service.createUser("test@example.com")
  result.email shouldBe "test@example.com"
}
```

## Debugging

### Enable Debug Logging

Add to your `log.properties`:

```
wvlet.uni.design=debug
```

This shows detailed information about binding resolution and object creation.

### Suppress Lifecycle Logging

For cleaner output in tests:

```scala
design
  .noLifeCycleLogging
  .build[App] { app => ... }
```

## Best Practices

1. **Use traits for interfaces** - Enables easy substitution in tests
2. **Create designs at the entry point** - Main method or test setup
3. **Pass dependencies through constructors** - Keeps classes testable and explicit
4. **Use singletons for shared resources** - Database pools, HTTP clients, caches
5. **Leverage lifecycle hooks** - For proper resource initialization and cleanup
6. **Compose designs** - Build modular, reusable design configurations
7. **Prefer tagged types over type aliases** - For type-safe multi-binding of same types

## Common Patterns

### Application Configuration

```scala
case class Config(host: String, port: Int)

class Server(config: Config):
  def start(): Unit =
    println(s"Starting server on ${config.host}:${config.port}")

val design = Design.newDesign
  .bindInstance[Config](Config("localhost", 8080))
  .bindSingleton[Server]

// Override for testing
val testDesign = design
  .bindInstance[Config](Config("localhost", randomPort()))
```

### Resource Management

```scala
class DatabaseConnection(config: DbConfig) extends AutoCloseable:
  private var conn: Connection = _

  def connect(): Unit =
    conn = DriverManager.getConnection(config.url)

  def query(sql: String): ResultSet =
    conn.createStatement().executeQuery(sql)

  override def close(): Unit =
    if conn != null then conn.close()

val design = Design.newDesign
  .bindSingleton[DatabaseConnection]
  .onStart(_.connect())
  // close() called automatically via AutoCloseable
```

### Multi-Handler Pattern

```scala
trait RequestHandler:
  def handle(request: Request): Response

class ApiHandler extends RequestHandler:
  def handle(request: Request): Response = ???

class StaticHandler extends RequestHandler:
  def handle(request: Request): Response = ???

class Router(apiHandler: ApiHandler, staticHandler: StaticHandler):
  def route(path: String, request: Request): Response =
    path match
      case p if p.startsWith("/api") => apiHandler.handle(request)
      case _ => staticHandler.handle(request)

val design = Design.newDesign
  .bindSingleton[ApiHandler]
  .bindSingleton[StaticHandler]
  .bindSingleton[Router]
```

## Known Limitations

### Type Aliases in Provider Bindings

Type aliases may not work correctly in provider bindings because they are resolved at compile time:

```scala
// This may cause issues
type MyString = String

Design.newDesign
  .bindInstance[MyString]("hello")
  .bindProvider[MyString, Service] { s => Service(s) }  // May fail
```

**Solution**: Use tagged types instead:

```scala
import wvlet.uni.design.tag.*

trait EnvTag

Design.newDesign
  .bindInstance[String @@ EnvTag]("hello")
  .bindProvider[String @@ EnvTag, Service] { s => Service(s) }  // Works correctly
```
