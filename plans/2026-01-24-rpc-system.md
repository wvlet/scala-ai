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
    appErrorCode: Option[Int] = None,
    metadata: Map[String, Any] = Map.empty
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
    appErrorCode: Option[Int] = None,
    metadata: Map[String, Any] = Map.empty
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
