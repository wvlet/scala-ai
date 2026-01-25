# Uni RPC System Design

## Problem Statement

Design an airframe-rpc like RPC system for Uni that:
1. **No RxRouterProvider boilerplate** - No companion objects needed
2. **Code generator for IDE support** - Generated client files for full code completion
3. **Simpler configuration** - Build config specifies RPC traits directly
4. **No annotations required** - Any trait can be an RPC interface
5. **Follow airframe-rpc patterns** - RPCStatus, RPCException design

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        RPC Trait                            │
│  trait UserService:                                         │
│    def getUser(id: Long): User                              │
└─────────────────────────────────────────────────────────────┘
                           │
          ┌────────────────┴────────────────┐
          ▼                                 ▼
┌─────────────────────┐         ┌─────────────────────────────┐
│   Server (JVM)      │         │   Code Generator (sbt)      │
│ RPCRouter.of[T]     │         │ → UserServiceClient.scala   │
└─────────────────────┘         └─────────────────────────────┘
```

## Implementation Plan

### Phase 1: RPC Status & Exception (Following Airframe Pattern)

#### 1.1 RPCStatusType
**File**: `uni/src/main/scala/wvlet/uni/http/rpc/RPCStatusType.scala`

```scala
sealed trait RPCStatusType:
  def prefix: String      // S, U, I, R
  def codeRange: (Int, Int)
  def isValidCode(code: Int): Boolean
  def isValidHttpStatus(httpStatus: HttpStatus): Boolean

object RPCStatusType:
  case object SUCCESS extends RPCStatusType           // S: 0-999
  case object USER_ERROR extends RPCStatusType        // U: 1000-1999, 4xx
  case object INTERNAL_ERROR extends RPCStatusType    // I: 2000-2999, 5xx
  case object RESOURCE_EXHAUSTED extends RPCStatusType // R: 3000-3999, 429
```

#### 1.2 RPCStatus
**File**: `uni/src/main/scala/wvlet/uni/http/rpc/RPCStatus.scala`

Following airframe naming: `{DESCRIPTION}_{TYPE}{NUMBER}`

```scala
sealed abstract class RPCStatus(val statusType: RPCStatusType):
  lazy val code: Int       // Derived from name suffix
  def name: String         // e.g., "INVALID_REQUEST_U1"
  def httpStatus: HttpStatus
  def isSuccess: Boolean
  def shouldReportStackTrace: Boolean
  def newException(message: String, cause: Throwable = null, ...): RPCException

object RPCStatus:
  // Success
  case object SUCCESS_S0 extends RPCStatus(SUCCESS)

  // User errors (non-retryable, 4xx)
  case object USER_ERROR_U0 extends RPCStatus(USER_ERROR)
  case object INVALID_REQUEST_U1 extends RPCStatus(USER_ERROR)
  case object INVALID_ARGUMENT_U2 extends RPCStatus(USER_ERROR)
  case object NOT_FOUND_U5 extends RPCStatus(USER_ERROR)
  case object UNAUTHENTICATED_U13 extends RPCStatus(USER_ERROR)
  case object PERMISSION_DENIED_U14 extends RPCStatus(USER_ERROR)
  // ... (all from airframe)

  // Internal errors (retryable, 5xx)
  case object INTERNAL_ERROR_I0 extends RPCStatus(INTERNAL_ERROR)
  case object UNKNOWN_I1 extends RPCStatus(INTERNAL_ERROR)
  case object UNAVAILABLE_I2 extends RPCStatus(INTERNAL_ERROR)
  // ...

  // Resource exhausted (429)
  case object RESOURCE_EXHAUSTED_R0 extends RPCStatus(RESOURCE_EXHAUSTED)
  case object EXCEEDED_RATE_LIMIT_R2 extends RPCStatus(RESOURCE_EXHAUSTED)
  // ...

  def ofCode(code: Int): RPCStatus
  def ofCodeName(name: String): RPCStatus
  def fromHttpStatus(httpStatus: HttpStatus): RPCStatus
```

#### 1.3 RPCException
**File**: `uni/src/main/scala/wvlet/uni/http/rpc/RPCException.scala`

```scala
case class RPCException(
    status: RPCStatus = RPCStatus.INTERNAL_ERROR_I0,
    message: String = "",
    cause: Option[Throwable] = None,
    appErrorCode: Option[Int] = None
) extends Exception:
  def noStackTrace: RPCException
  def shouldReportStackTrace: Boolean
  def toMessage: RPCErrorMessage
  def toJson: String
  def toMsgPack: MsgPack
  def toResponse: Response

case class RPCErrorMessage(
    code: Int,
    codeName: String,
    message: String,
    appErrorCode: Option[Int] = None
) derives ObjectWeaver

object RPCException:
  def fromJson(json: String): RPCException
  def fromMsgPack(msgpack: MsgPack): RPCException
  def fromResponse(response: Response): RPCException
```

### Phase 2: Server-Side Implementation

#### 2.1 RPCRouter
**File**: `uni/src/main/scala/wvlet/uni/http/rpc/RPCRouter.scala`

```scala
object RPCRouter:
  inline def of[T]: Router = RPCRouterMacros.buildRPCRouter[T]
  inline def of[T](prefix: String): Router = RPCRouterMacros.buildRPCRouter[T](prefix)
```

All public methods → `POST /rpc/{ServiceName}/{methodName}`

#### 2.2 RPCRequestMapper
**File**: `uni/src/main/scala/wvlet/uni/http/rpc/RPCRequestMapper.scala`

- Parse JSON body as positional args `[arg1, arg2]` or named args `{"param": val}`
- Deserialize using `ObjectWeaver` based on parameter Surface

#### 2.3 RPCHandler
**File**: `uni-netty/src/main/scala/wvlet/uni/http/rpc/RPCHandler.scala`

Extends `RouterHandler`:
1. Parse JSON request body
2. Invoke method via `MethodSurface.call()`
3. Serialize response with `ObjectWeaver.toJson()`
4. Map exceptions to `RPCException.toResponse()`

### Phase 3: Code Generator

#### 3.1 sbt Plugin Configuration
**File**: `sbt-uni/src/main/scala/wvlet/uni/sbt/UniRPCPlugin.scala`

```scala
// build.sbt
uniRPCClients := Seq(
  "com.example.api.UserService",
  "com.example.api.OrderService"
)
```

#### 3.2 Client Generator
**File**: `uni-rpc-codegen/src/main/scala/wvlet/uni/rpc/codegen/RPCClientGenerator.scala`

Generates sync and async client classes:

```scala
// Generated: UserServiceClient.scala
class UserServiceClient(httpClient: HttpSyncClient, baseUrl: String):
  def getUser(id: Long): User =
    val response = httpClient.send(
      HttpRequest.post(s"${baseUrl}/rpc/UserService/getUser")
        .withJsonContent(ObjectWeaver.toJson(Seq(id)))
    )
    handleResponse[User](response)

  private def handleResponse[T: ObjectWeaver](response: HttpResponse): T =
    if response.status.isSuccessful then
      ObjectWeaver.fromJson[T](response.contentAsString)
    else
      throw RPCException.fromResponse(response)

class UserServiceAsyncClient(httpClient: HttpAsyncClient, baseUrl: String):
  def getUser(id: Long): Rx[User] = ...
```

## File Structure

```
uni/src/main/scala/wvlet/uni/http/rpc/
├── RPCStatusType.scala       # Status type enum (SUCCESS, USER_ERROR, etc.)
├── RPCStatus.scala           # Status codes (INVALID_REQUEST_U1, etc.)
├── RPCException.scala        # Exception + RPCErrorMessage
├── RPCRouter.scala           # Router builder
├── RPCRouterMacros.scala     # Route extraction macros
└── RPCRequestMapper.scala    # JSON arg binding

uni-netty/src/main/scala/wvlet/uni/http/rpc/
└── RPCHandler.scala          # Request handler

sbt-uni/src/main/scala/wvlet/uni/sbt/
├── UniRPCPlugin.scala        # sbt plugin
└── UniRPCKeys.scala          # sbt settings

uni-rpc-codegen/src/main/scala/wvlet/uni/rpc/codegen/
├── RPCClientGenerator.scala  # Code generator
└── RPCCodeGenMain.scala      # CLI entry point
```

## Example Usage

### Define RPC Trait
```scala
// No annotation needed
trait GreeterService:
  def hello(name: String): String
  def greetAll(names: Seq[String]): Seq[String]
```

### Server
```scala
class GreeterServiceImpl extends GreeterService:
  def hello(name: String): String = s"Hello, ${name}!"
  def greetAll(names: Seq[String]): Seq[String] = names.map(hello)

val router = RPCRouter.of[GreeterService]
val server = NettyServer(8080)
  .withHandler(RPCHandler(router, GreeterServiceImpl()))
```

### Build Config
```scala
uniRPCClients := Seq("com.example.GreeterService")
```

### Client (Generated)
```scala
val greeter = GreeterServiceClient(httpClient, "http://localhost:8080")
greeter.hello("World")  // Full IDE support
```

## Implementation Sequence

1. **RPCStatusType + RPCStatus** - Port from airframe pattern
2. **RPCException + RPCErrorMessage** - Port from airframe pattern
3. **RPCRouter + RPCRouterMacros** - Adapt from existing RouterMacros
4. **RPCRequestMapper** - JSON body parsing
5. **RPCHandler** - Netty integration
6. **RPCClientGenerator** - Code generation
7. **sbt-uni Plugin** - Build integration

## Verification

1. `./sbt compile` - Compile all modules
2. `./sbt "uniJVM/testOnly *RPC*"` - Unit tests
3. Code generation test - Verify generated client compiles
4. IDE test - Verify code completion in IntelliJ
5. Integration test - Server + client end-to-end

---

## Implementation Notes (2026-01-25)

### Status

- **Phase 1 (RPCStatus & Exception)**: ✅ Complete
- **Phase 2 (Server-Side)**: 🔲 Deferred - removed half-baked implementation
- **Phase 3 (Code Generator)**: 🔲 Pending

PR: https://github.com/wvlet/uni/pull/367

### What's Implemented

Files in `uni/src/main/scala/wvlet/uni/http/rpc/`:
- `RPCStatusType.scala` - Scala 3 enum for status categories
- `RPCStatus.scala` - Scala 3 enum for 30+ status codes
- `RPCException.scala` - Exception with JSON serialization

Test: `uni/.jvm/src/test/scala/wvlet/uni/http/rpc/RPCStatusTest.scala` (27 tests)

### Design Changes from Original Plan

#### 1. Scala 3 Enums for RPCStatusType and RPCStatus

Changed from sealed trait/abstract class to Scala 3 enums for cleaner code:

```scala
// RPCStatusType as Scala 3 enum
enum RPCStatusType(val prefix: String, val minCode: Int, val maxCode: Int):
  case SUCCESS extends RPCStatusType("S", 0, 1000)
  case USER_ERROR extends RPCStatusType("U", 1000, 2000)
  case INTERNAL_ERROR extends RPCStatusType("I", 2000, 3000)
  case RESOURCE_EXHAUSTED extends RPCStatusType("R", 3000, 4000)

// RPCStatus as Scala 3 enum
enum RPCStatus(val statusType: RPCStatusType, val httpStatus: HttpStatus):
  case SUCCESS_S0 extends RPCStatus(RPCStatusType.SUCCESS, HttpStatus.Ok_200)
  case INVALID_REQUEST_U1 extends RPCStatus(RPCStatusType.USER_ERROR, HttpStatus.BadRequest_400)
  // ... 30+ status codes
```

Benefits:
- Built-in `values` method returns all cases
- Built-in `valueOf` for name lookup
- Pattern matching exhaustivity checks
- Cleaner, more idiomatic Scala 3

#### 2. Naming Decision: Keep `RPC` Prefix

Considered alternatives: `ServiceStatus`, `AppStatus`, `ErrorStatus`, bare `Status`.

**Decision: Keep `RPCStatus`, `RPCStatusType`, `RPCException`**

Rationale:
- Aligns with package structure (`wvlet.uni.http.rpc`)
- Matches wire protocol header (`X-RPC-Status`)
- Avoids confusion/collision with `HttpStatus`
- Familiar to airframe-rpc users
- RPC is conceptually a superset - REST is RPC over HTTP with conventions

Note: These can be used for REST/GraphQL servers too. If broader naming is needed
later, add type aliases (`type ServiceStatus = RPCStatus`) without breaking changes.

#### 3. Code Range Design (1000-sized buckets)

Considered alternative: smaller ranges like 0-99, 100-199, 200-299, 300-399.

**Decision: Keep 1000-sized ranges (0-999, 1000-1999, 2000-2999, 3000-3999)**

| Range | Category | Prefix | Current Count |
|-------|----------|--------|---------------|
| 0-999 | SUCCESS | S | 1 |
| 1000-1999 | USER_ERROR | U | 15 |
| 2000-2999 | INTERNAL_ERROR | I | 9 |
| 3000-3999 | RESOURCE_EXHAUSTED | R | 9 |

**Rationale:**

1. **No HTTP status code collision**
   - Codes like `1001`, `2000`, `3000` are clearly distinct from HTTP `200`, `400`, `500`
   - Using 0-399 would overlap with HTTP 1xx/2xx/3xx causing confusion in logs/headers

2. **Leading digit identifies category**
   - `1xxx` = User error (client mistake, don't retry)
   - `2xxx` = Internal error (server issue, may retry)
   - `3xxx` = Resource exhausted (throttling, retry with backoff)

3. **Wire format compatibility**
   - Numeric code is sent in `X-RPC-Status` header and `RPCErrorMessage.code`
   - Changing ranges would be a breaking wire protocol change

4. **Sufficient extensibility**
   - Current: ~34 codes used out of 4000 available
   - 1000 per category costs nothing and leaves room for future standard RPC statuses
   - `appErrorCode` field handles application-specific error codes separately

5. **Comparison with other systems**
   - gRPC uses 0-16 (tiny fixed set) - not comparable since our design already exceeds that
   - HTTP uses 100-599 - our ranges intentionally avoid this space
   - airframe-rpc uses same 1000-sized ranges - proven design

#### 4. RPCException Immutability

Original design had mutable `includeStackTrace` var. Changed to immutable case class:

```scala
case class RPCException(
    status: RPCStatus = RPCStatus.INTERNAL_ERROR_I0,
    message: String = "",
    cause: Option[Throwable] = None,
    appErrorCode: Option[Int] = None,
    private val includeStackTrace: Option[Boolean] = None  // In constructor
) extends Exception:
  def noStackTrace: RPCException = copy(includeStackTrace = Some(false))
```

#### 5. Server-Side Implementation Deferred

Removed half-baked RPCRouter, RPCHandler, RPCRequestMapper. These require:
- ObjectWeaver support for complex object parameters (code generator needed)
- More thorough design for request/response handling
- Better integration with existing Router infrastructure

#### 6. RPCErrorMessage Simplified (metadata field removed)

Original airframe-rpc design included a `metadata: Map[String, Any]` field for
arbitrary error context. Removed for simplicity:

- Primitive types only - no `Map[String, Any]` support in ObjectWeaver without
  custom codec
- `appErrorCode: Option[Int]` covers the main use case (application-specific codes)
- Can add structured metadata fields later if needed (e.g., `retryAfterMs: Option[Long]`)

```scala
// Current (simplified)
case class RPCErrorMessage(
    code: Int,
    codeName: String,
    message: String,
    appErrorCode: Option[Int] = None
)

// Future (if needed)
case class RPCErrorMessage(
    code: Int,
    codeName: String,
    message: String,
    appErrorCode: Option[Int] = None,
    retryAfterMs: Option[Long] = None,  // Rate limit hint
    details: Option[String] = None       // Additional context
)
```

#### 5. Complex Object Parameters Not Supported

ObjectWeaver requires compile-time types via `derived[A]` macro. Cannot deserialize
complex objects at runtime using Surface alone. This will be addressed by the code
generator which can generate type-specific deserializers.

```scala
case obj: JSONObject =>
  throw RPCStatus.INVALID_ARGUMENT_U2.newException(
    s"Parameter '${paramName}' has unsupported type ${surface.name}. " +
      "Complex object parameters are not yet supported in RPC."
  )
```

### Key Learnings

#### 1. Scala 3 Enum Benefits

Using Scala 3 enums instead of sealed traits:
- `RPCStatusType.values` returns `Array[RPCStatusType]` automatically
- `RPCStatus.values` returns all 30+ status codes
- Pattern matching with exhaustivity checking
- No need for manual `all` method maintenance

#### 2. Error Code Derivation from Name

Status codes are derived from the enum name suffix:
```scala
lazy val code: Int = RPCStatus.extractErrorCode(toString)
// "INVALID_REQUEST_U1" -> U=1000, 1 -> 1001
// "INTERNAL_ERROR_I0" -> I=2000, 0 -> 2000
```

#### 3. ObjectWeaver Compile-Time Requirement

ObjectWeaver uses Scala 3 macros and requires compile-time type information:
- ✅ `ObjectWeaver.derived[MyClass]` - Works (compile-time)
- ❌ `ObjectWeaver.fromSurface(surface)` - Does not exist (runtime)

This is why complex object parameters need code generation for server-side RPC.

### Current Limitations

1. **Server-side RPC not implemented**: RPCRouter/RPCHandler deferred until code generator
2. **MsgPack serialization**: RPCException supports it but not fully tested
3. **Map types**: `Map[String, Any]` not directly supported by ObjectWeaver

### ObjectWeaver Requirements for Full RPC Support

ObjectWeaver currently works well for:
- ✅ Primitive types (Int, Long, String, Boolean, etc.)
- ✅ Option types
- ✅ Seq/List types with primitive elements
- ✅ Case classes with `ObjectWeaver.derived[A]`

What would be needed for full RPC complex object support:

#### Option A: Code Generator (Recommended)
Generate ObjectWeaver instances at compile-time for RPC parameter types:

```scala
// Generated by sbt plugin
object UserServiceWeavers:
  given userWeaver: ObjectWeaver[User] = ObjectWeaver.derived[User]
  given addressWeaver: ObjectWeaver[Address] = ObjectWeaver.derived[Address]
```

#### Option B: Runtime ObjectWeaver from Surface (Not Currently Possible)
Would require ObjectWeaver enhancement:

```scala
// Hypothetical API - does not exist
object ObjectWeaver:
  def fromSurface[A](surface: Surface): ObjectWeaver[A] = ???
```

This is challenging because:
- Scala 3 macros run at compile-time only
- Surface provides runtime type info but ObjectWeaver needs compile-time derivation
- Would need a different approach (e.g., runtime reflection-based codec)

#### Option C: JSON AST Pass-through
For advanced use cases, pass JSONObject/JSONArray directly:

```scala
trait AdvancedService:
  def processJson(data: JSONObject): JSONObject  // Works today
```

**Recommendation**: Option A (code generator) is the cleanest solution and aligns
with the original design goal of IDE support through generated clients.

### Future Work (Phase 3)

1. **Code Generator** - Generate type-safe clients with full IDE support
2. **Complex Object Support** - Generate parameter deserializers
3. **MsgPack Support** - Add binary serialization option
4. **Cross-Platform Client** - HTTP client for JS/Native
