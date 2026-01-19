# Dependency Injection

uni provides a compile-time safe dependency injection framework through `Design`.

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

Bind an interface to its implementation:

```scala
design.bindImpl[Service, ServiceImpl]
```

### Instance Binding

Bind a pre-created instance:

```scala
val config = Config("localhost", 8080)
design.bindInstance[Config](config)
```

### Singleton Binding

Ensure only one instance is created:

```scala
design.bindSingleton[DatabasePool]
```

### Provider Binding

Use a factory function with dependencies:

```scala
design.bindProvider[Config, HttpClient] { config =>
  HttpClient(config.host, config.port)
}
```

Multiple dependencies are also supported:

```scala
design.bindProvider[Config, Logger, HttpClient] { (config, logger) =>
  logger.info(s"Creating client for ${config.host}")
  HttpClient(config.host, config.port)
}
```

## Lifecycle Management

### Startup and Shutdown Hooks

```scala
val design = Design.newDesign
  .bindSingleton[DatabasePool]
  .onStart { pool =>
    pool.connect()
  }
  .onShutdown { pool =>
    pool.close()
  }
```

### Session Scope

Sessions manage the lifecycle of bound objects:

```scala
design.withSession { session =>
  // Objects are created and started
  val pool = session.build[DatabasePool]

  // Use the pool...

} // All objects are shutdown in reverse order
```

## Design Composition

Designs can be combined:

```scala
val baseDesign = Design.newDesign
  .bindImpl[Logger, ConsoleLogger]

val dbDesign = Design.newDesign
  .bindImpl[DatabaseService, PostgresDatabase]

val combined = baseDesign + dbDesign
```

## Testing

Create test-specific designs:

```scala
// Production design
val prodDesign = Design.newDesign
  .bindImpl[UserRepository, PostgresUserRepository]

// Test design with mock
val testDesign = Design.newDesign
  .bindImpl[UserRepository, InMemoryUserRepository]

// Use in tests
testDesign.build[UserService] { service =>
  val result = service.getUser("test-id")
  result.name shouldBe "Test User"
}
```

## Best Practices

1. **Use traits for dependencies** - Makes testing easier
2. **Bind at the edge** - Create designs in main/test entry points
3. **Prefer constructor injection** - Dependencies as constructor parameters
4. **Use singletons for shared resources** - Database pools, HTTP clients
5. **Leverage lifecycle hooks** - For proper resource cleanup
