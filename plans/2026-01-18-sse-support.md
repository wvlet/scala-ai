# SSE (Server-Sent Events) Support for uni-http

## Overview

Add Server-Sent Events (SSE) support to uni-http, following the pattern from airframe-http. This will enable:
1. **Client-side**: Consuming SSE streams from servers
2. **Server-side**: (Future) Producing SSE streams (requires uni-http-netty integration)

## Scope

This PR focuses on **client-side SSE support** (parsing SSE streams and providing a callback-based handler interface). Server-side SSE production is out of scope for this PR.

## Files to Create/Modify

### 1. Create `ServerSentEvent.scala` (New file)
**Path**: `uni/src/main/scala/wvlet/uni/http/ServerSentEvent.scala`

```scala
case class ServerSentEvent(
    id: Option[String] = None,
    event: Option[String] = None,
    retry: Option[Long] = None,
    data: String
)
```

Key features:
- `toContent`: String - Serialize to SSE wire format (`id: ...\nevent: ...\ndata: ...\n\n`)
- Companion object with `parse(line: String)` for parsing individual lines

### 2. Create `ServerSentEventHandler.scala` (New file)
**Path**: `uni/src/main/scala/wvlet/uni/http/ServerSentEventHandler.scala`

```scala
trait ServerSentEventHandler:
  def onConnect(response: HttpResponse): Unit = ()
  def onEvent(e: ServerSentEvent): Unit
  def onError(e: Throwable): Unit = ()
  def onCompletion(): Unit = ()
```

Key features:
- Callback-based handler for SSE events
- Default empty handler via `ServerSentEventHandler.empty`

### 3. Create `ServerSentEventParser.scala` (New file)
**Path**: `uni/src/main/scala/wvlet/uni/http/ServerSentEventParser.scala`

Stateful parser for SSE wire protocol:
- Accumulates lines until empty line (event boundary)
- Handles multi-line `data:` fields
- Handles `id:`, `event:`, `retry:` fields
- Handles comments (lines starting with `:`)

### 4. Modify `ContentType.scala`
**Path**: `uni/src/main/scala/wvlet/uni/http/ContentType.scala`

Add extension methods:
```scala
def isEventStream: Boolean = fullType == "text/event-stream"
```

Note: `TextEventStream` constant already exists at line 47.

### 5. Modify `Request.scala`
**Path**: `uni/src/main/scala/wvlet/uni/http/Request.scala`

Add SSE-related methods:
```scala
case class Request(
    ...
    eventHandler: ServerSentEventHandler = ServerSentEventHandler.empty
)

def withEventHandler(handler: ServerSentEventHandler): Request
def withAcceptEventStream: Request  // Sets Accept: text/event-stream
```

### 6. Modify `HttpAsyncClient` trait
**Path**: `uni/src/main/scala/wvlet/uni/http/HttpClient.scala`

Add SSE streaming method:
```scala
def sendSSE(request: HttpRequest): Rx[ServerSentEvent]
```

### 7. Modify `DefaultHttpClient.scala`
**Path**: `uni/src/main/scala/wvlet/uni/http/DefaultHttpClient.scala`

Implement `sendSSE` that:
- Uses `sendStreaming` to get byte chunks
- Parses chunks through `ServerSentEventParser`
- Emits `ServerSentEvent` objects
- Invokes handler callbacks

### 8. Create test file
**Path**: `uni/src/test/scala/wvlet/uni/http/ServerSentEventTest.scala`

Test cases:
- `ServerSentEvent.toContent` generates correct format
- `ServerSentEventParser` correctly parses:
  - Simple data events
  - Multi-line data events
  - Events with id, event type, retry
  - Comments (ignored)
  - Multiple events in stream

## Implementation Details

### SSE Wire Protocol (RFC 6657)

Format:
```
id: <event-id>
event: <event-type>
retry: <milliseconds>
data: <line1>
data: <line2>

```

Rules:
- Lines are separated by `\n` or `\r\n`
- Events are separated by blank lines (`\n\n`)
- Lines starting with `:` are comments (ignored)
- Multiple `data:` lines are concatenated with `\n`
- Fields are: `id`, `event`, `data`, `retry`

### Parser State Machine

```
IDLE -> reading lines -> ACCUMULATING
     |                        |
     v                        v (blank line)
   EMIT                    EMIT event
```

## Verification

1. **Unit tests**: Run `sbt "uni/testOnly *ServerSentEvent*"`
2. **Format check**: Run `sbt scalafmtAll`
3. **Full compile**: Run `sbt compile`
4. **All tests**: Run `sbt test`

## Dependencies

- No new external dependencies
- Uses existing `Rx` from `uni-core`
- Uses existing `HttpContent`, `ContentType` from `uni`
