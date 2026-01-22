# Plan: REST Router Framework for uni-netty

## Overview

Implement a REST server framework similar to airframe-http that allows defining HTTP endpoints using Scala annotations and provides route matching with filter composition.

## Requirements

1. `@Endpoint(method, path)` annotation for marking controller methods
2. `Router.of[Controller]` syntax for registering controllers
3. `Router.filter[X].andThen(Router.of[...])` for filter chaining
4. No class-level path prefix needed
5. Support path parameters (`:id` syntax)
6. Integration with existing `RxHttpHandler`/`RxHttpFilter`

## Module Location

All files will be created in: `uni/src/main/scala/wvlet/uni/http/router/`

This places the router in the cross-platform `uni` module, making it available for JVM, JS, and Native.

## Files Created

### Core Types (Phase 1)
- `Endpoint.scala` - Annotation definition
- `Route.scala` - Route data structure with PathComponent

### Router Builder (Phase 2)
- `Router.scala` - Router builder with `of[T]` and `filter[F]` APIs
- `RouterMacros.scala` - Compile-time route extraction using Surface

### Route Matching (Phase 3)
- `RouteMatcher.scala` - Route matching with path parameter extraction

### Request Mapping (Phase 4)
- `HttpRequestMapper.scala` - Parameter binding from path/query/defaults

### Response Handling (Phase 5)
- `ResponseConverter.scala` - Converts return values to HTTP responses

### Integration (Phase 6)
- `ControllerProvider.scala` - Controller instance providers
- `RouterHandler.scala` (in uni-netty) - RxHttpHandler integration

## Example Usage

```scala
import wvlet.uni.http.router.*
import wvlet.uni.http.{HttpMethod, Request, Response}
import wvlet.uni.netty.{NettyServer, RouterHandler}

class UserController:
  @Endpoint(HttpMethod.GET, "/users")
  def listUsers(): Seq[User] = ???

  @Endpoint(HttpMethod.GET, "/users/:id")
  def getUser(id: String): User = ???

  @Endpoint(HttpMethod.POST, "/users")
  def createUser(request: Request): User = ???

// Build router
val router = Router.of[UserController]

// With filter
val routerWithFilter = Router
  .filter[LogFilter]
  .andThen(Router.of[UserController])

// Start server
NettyServer
  .withPort(8080)
  .withRxHandler(RouterHandler(router))
  .start()
```

## Key Design Decisions

1. **Annotation-based routing**: Uses `@Endpoint(method, path)` annotation for clean endpoint definitions
2. **Path parameters**: Supports `:paramName` syntax for extracting path segments
3. **Parameter binding order**: path params -> query params -> method defaults -> Request object
4. **Response conversion**: Automatically converts return values to HTTP responses (JSON for objects, text for strings)
5. **Filter composition**: Filters can be composed using `Router.filter[F].andThen(router)`
6. **Surface-based reflection**: Uses uni's Surface API for compile-time method discovery

## Verification

Tests are provided in `uni/src/test/scala/wvlet/uni/http/router/RouterTest.scala` covering:
- Path pattern parsing
- Route extraction from controllers
- Route matching
- Parameter binding
- Response conversion
- Controller providers
