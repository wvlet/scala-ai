# HTTP Client

uni provides both synchronous and asynchronous HTTP clients.

## Creating Clients

### Sync Client

```scala
import wvlet.uni.http.Http

val client = Http.client.newSyncClient
```

### Async Client

```scala
val asyncClient = Http.client.newAsyncClient
```

## Making Requests

### GET Request

```scala
import wvlet.uni.http.Request

val response = client.send(Request.get("https://api.example.com/users"))
println(response.status)           // HttpStatus
println(response.contentAsString)  // Response body
```

### POST Request

```scala
val request = Request
  .post("https://api.example.com/users")
  .withJsonContent("""{"name": "Alice"}""")

val response = client.send(request)
```

### With Headers

```scala
val request = Request
  .get("https://api.example.com/data")
  .withBearerToken("token123")
  .withAccept("application/json")

val response = client.send(request)
```

## Response Handling

```scala
val response = client.send(request)

// Status
response.status.isSuccess    // true for 2xx
response.status.code         // 200, 404, etc.

// Headers
response.header("Content-Type")  // Option[String]

// Body
response.contentAsString     // String
response.contentAsBytes      // Array[Byte]
```

## Async Requests

```scala
val asyncClient = Http.client.newAsyncClient

asyncClient
  .send(Request.get("https://api.example.com/data"))
  .map { response =>
    response.contentAsString
  }
  .subscribe { content =>
    println(content)
  }
```

## Streaming Responses

Stream large responses as byte chunks:

```scala
asyncClient
  .sendStreaming(Request.get("https://example.com/large-file"))
  .subscribe { chunk: Array[Byte] =>
    processChunk(chunk)
  }
```

## Client Configuration

```scala
val client = Http.client
  .withConnectTimeoutMillis(5000)   // 5 seconds
  .withReadTimeoutMillis(30000)     // 30 seconds
  .withMaxRetry(3)
  .newSyncClient
```

## Disabling Retry

```scala
// Create a client with no retries
val clientNoRetry = Http.client.noRetry.newSyncClient

// Or configure zero retries
val clientNoRetry2 = Http.client.withMaxRetry(0).newSyncClient
```

## Error Handling

```scala
import wvlet.uni.http.HttpException

try
  val response = client.send(request)
  if !response.status.isSuccess then
    throw HttpException(response.status, response.contentAsString)
catch
  case e: HttpException =>
    logger.error(s"HTTP error: ${e.status}")
  case e: IOException =>
    logger.error("Network error", e)
```

## Best Practices

1. **Reuse clients** - Create once, use many times
2. **Close clients** - Call `close()` when done
3. **Set timeouts** - Prevent hanging requests
4. **Handle errors** - Check status codes
5. **Use async** - For non-blocking operations

## Example: REST Client

```scala
class ApiClient(baseUrl: String):
  private val client = Http.client.newSyncClient

  def getUser(id: String): User =
    val response = client.send(
      Request.get(s"${baseUrl}/users/${id}")
    )
    if response.status.isSuccess then
      Weaver.fromJson[User](response.contentAsString)
    else
      throw HttpException(response.status)

  def createUser(user: User): User =
    val response = client.send(
      Request
        .post(s"${baseUrl}/users")
        .withJsonContent(Weaver.toJson(user))
    )
    Weaver.fromJson[User](response.contentAsString)

  def close(): Unit = client.close()
```
