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
import wvlet.uni.http.HttpRequest

val response = client.send(HttpRequest.get("https://api.example.com/users"))
println(response.status)           // HttpStatus
println(response.contentAsString)  // Response body
```

### POST Request

```scala
val request = HttpRequest
  .post("https://api.example.com/users")
  .withContentType("application/json")
  .withContent("""{"name": "Alice"}""")

val response = client.send(request)
```

### With Headers

```scala
val request = HttpRequest
  .get("https://api.example.com/data")
  .withHeader("Authorization", "Bearer token123")
  .withHeader("Accept", "application/json")

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
  .send(HttpRequest.get("https://api.example.com/data"))
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
  .sendStreaming(HttpRequest.get("https://example.com/large-file"))
  .subscribe { chunk: Array[Byte] =>
    processChunk(chunk)
  }
```

## Client Configuration

```scala
import wvlet.uni.http.HttpClientConfig

val config = HttpClientConfig()
  .withConnectTimeout(5000)   // 5 seconds
  .withReadTimeout(30000)     // 30 seconds
  .withMaxRetries(3)

val client = Http.client
  .withConfig(config)
  .newSyncClient
```

## Disabling Retry

```scala
// Disable retry for a specific request
val response = client.noRetry.send(request)

// Or configure zero retries
val clientNoRetry = client.withMaxRetry(0)
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
      HttpRequest.get(s"${baseUrl}/users/${id}")
    )
    if response.status.isSuccess then
      ObjectWeaver.fromJSON[User](response.contentAsString)
    else
      throw HttpException(response.status)

  def createUser(user: User): User =
    val response = client.send(
      HttpRequest
        .post(s"${baseUrl}/users")
        .withContentType("application/json")
        .withContent(ObjectWeaver.toJSON(user))
    )
    ObjectWeaver.fromJSON[User](response.contentAsString)

  def close(): Unit = client.close()
```
