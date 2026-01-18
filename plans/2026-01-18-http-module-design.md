# HTTP Module Design for wvlet/uni

## Overview

This document outlines the design for a cross-platform HTTP module in wvlet/uni, providing foundational abstractions for HTTP request/response handling, status codes, content types, sync/async client interfaces, and exception types. This module serves as the base layer for implementing Netty-based servers, RPC systems, and cross-platform HTTP/RPC clients.

## Design Principles

1. **Cross-Platform Compatibility**: Core abstractions work on JVM, Scala.js, and Scala Native
2. **Minimal Dependencies**: Pure Scala implementation for shared code
3. **Builder Pattern**: Configuration classes use `withXxx()` methods following uni conventions
4. **Layered Architecture**: Low-level primitives that higher-level libraries build upon
5. **Type Safety**: Leverage Scala 3's type system for compile-time correctness
6. **Zero Boilerplate**: Intuitive API inspired by Python's requests and Rust's reqwest

## Research Summary

### Inspiration from Other Libraries

| Library | Language | Key Design Insights |
|---------|----------|---------------------|
| requests/httpx | Python | Simple API, session management, sync/async duality |
| reqwest/hyper | Rust | Layered architecture, connection pooling, feature flags |
| net/http | Go | Interface-based design, handler pattern, middleware support |
| airframe-http | Scala | Status codes with categories, RPC integration, Netty backend |

### Key Takeaways

- **Python requests**: Ergonomic API with `response.status_code`, `response.json()`, session objects
- **Rust reqwest**: Client reuse for connection pooling, async-first with sync wrapper
- **Go net/http**: Interface segregation, handler functions, middleware chain pattern
- **airframe-http**: Status code classification, RPCStatus for retry logic, MessagePack support

## Module Structure

```
uni/
  src/main/scala/wvlet/uni/http/
    # Core Types (Pure Scala - Cross Platform)
    HttpMethod.scala        # HTTP method enum
    HttpStatus.scala        # Status codes with categories
    HttpHeader.scala        # Header name/value pairs
    HttpHeaders.scala       # Header collection
    ContentType.scala       # MIME type abstraction

    # Request/Response
    HttpRequest.scala       # Immutable request representation
    HttpResponse.scala      # Immutable response representation
    HttpMessage.scala       # Common message trait
    HttpContent.scala       # Body content abstraction

    # Client Interfaces
    HttpClient.scala        # Sync client trait
    AsyncHttpClient.scala   # Async client trait
    HttpClientConfig.scala  # Client configuration

    # Server Interfaces (for future Netty integration)
    HttpHandler.scala       # Request handler trait
    HttpFilter.scala        # Middleware/filter abstraction

    # Exceptions
    HttpException.scala     # Base exception hierarchy

  .jvm/src/main/scala/wvlet/uni/http/
    # JVM-specific implementations
    platform/
      JVMHttpClient.scala   # URLConnection-based client

  .js/src/main/scala/wvlet/uni/http/
    # Scala.js-specific implementations
    platform/
      JSHttpClient.scala    # Fetch API-based client

  .native/src/main/scala/wvlet/uni/http/
    # Scala Native-specific implementations
    platform/
      NativeHttpClient.scala  # libcurl-based client (future)
```

## Core Types

### 1. HttpMethod

```scala
package wvlet.uni.http

enum HttpMethod(val name: String):
  case GET     extends HttpMethod("GET")
  case POST    extends HttpMethod("POST")
  case PUT     extends HttpMethod("PUT")
  case DELETE  extends HttpMethod("DELETE")
  case PATCH   extends HttpMethod("PATCH")
  case HEAD    extends HttpMethod("HEAD")
  case OPTIONS extends HttpMethod("OPTIONS")
  case TRACE   extends HttpMethod("TRACE")
  case CONNECT extends HttpMethod("CONNECT")

object HttpMethod:
  def of(name: String): Option[HttpMethod] =
    values.find(_.name.equalsIgnoreCase(name))

  def ofUnsafe(name: String): HttpMethod =
    of(name).getOrElse(throw IllegalArgumentException(s"Unknown HTTP method: ${name}"))
```

### 2. HttpStatus

Status codes follow RFC 9110 as a Scala 3 enum with classification helpers for retry logic:

```scala
package wvlet.uni.http

enum HttpStatus(val code: Int, val reason: String):
  // 1xx Informational
  case Continue_100           extends HttpStatus(100, "Continue")
  case SwitchingProtocols_101 extends HttpStatus(101, "Switching Protocols")
  case Processing_102         extends HttpStatus(102, "Processing")
  case EarlyHints_103         extends HttpStatus(103, "Early Hints")

  // 2xx Success
  case Ok_200                   extends HttpStatus(200, "OK")
  case Created_201              extends HttpStatus(201, "Created")
  case Accepted_202             extends HttpStatus(202, "Accepted")
  case NonAuthoritativeInfo_203 extends HttpStatus(203, "Non-Authoritative Information")
  case NoContent_204            extends HttpStatus(204, "No Content")
  // ... more 2xx codes

  // 3xx Redirection
  case MultipleChoices_300   extends HttpStatus(300, "Multiple Choices")
  case MovedPermanently_301  extends HttpStatus(301, "Moved Permanently")
  case Found_302             extends HttpStatus(302, "Found")
  // ... more 3xx codes

  // 4xx Client Error
  case BadRequest_400        extends HttpStatus(400, "Bad Request")
  case Unauthorized_401      extends HttpStatus(401, "Unauthorized")
  case NotFound_404          extends HttpStatus(404, "Not Found")
  // ... more 4xx codes

  // 5xx Server Error
  case InternalServerError_500 extends HttpStatus(500, "Internal Server Error")
  case ServiceUnavailable_503  extends HttpStatus(503, "Service Unavailable")
  // ... more 5xx codes

  // Unknown status holder for non-standard codes (parameterized case)
  case Unknown(override val code: Int) extends HttpStatus(code, "Unknown")

  def isInformational: Boolean = code >= 100 && code < 200
  def isSuccessful: Boolean    = code >= 200 && code < 300
  def isRedirection: Boolean   = code >= 300 && code < 400
  def isClientError: Boolean   = code >= 400 && code < 500
  def isServerError: Boolean   = code >= 500 && code < 600

  def isRetryable: Boolean = this match
    case ServiceUnavailable_503 | GatewayTimeout_504 |
         TooManyRequests_429 | RequestTimeout_408 => true
    case _ => false

  override def toString: String = s"${code} ${reason}"

end HttpStatus

object HttpStatus:
  // Note: Due to parameterized Unknown case, we manually enumerate known statuses
  // instead of using the auto-generated `values` method
  val knownStatuses: Seq[HttpStatus] = Seq(
    Continue_100, SwitchingProtocols_101, Processing_102, EarlyHints_103,
    Ok_200, Created_201, /* ... all status codes ... */
    InternalServerError_500, ServiceUnavailable_503, /* ... */
    NetworkAuthRequired_511
  )

  private val statusByCode: Map[Int, HttpStatus] =
    knownStatuses.map(s => s.code -> s).toMap

  def ofCode(code: Int): HttpStatus =
    statusByCode.getOrElse(code, Unknown(code))

  def unapply(code: Int): Option[HttpStatus] = Some(ofCode(code))

end HttpStatus
```

### 3. ContentType

ContentType is implemented as an opaque type wrapping a string for efficiency. This avoids object allocation overhead while still providing type safety and extension methods.

```scala
package wvlet.uni.http

opaque type ContentType = String

object ContentType:
  // Constructor
  def apply(value: String): ContentType = value

  // Application types
  val ApplicationJson: ContentType           = "application/json"
  val ApplicationXml: ContentType            = "application/xml"
  val ApplicationFormUrlEncoded: ContentType = "application/x-www-form-urlencoded"
  val ApplicationOctetStream: ContentType    = "application/octet-stream"
  val ApplicationMsgPack: ContentType        = "application/msgpack"
  val ApplicationPdf: ContentType            = "application/pdf"
  val ApplicationZip: ContentType            = "application/zip"
  val ApplicationGzip: ContentType           = "application/gzip"

  // Text types
  val TextPlain: ContentType       = "text/plain"
  val TextHtml: ContentType        = "text/html"
  val TextCss: ContentType         = "text/css"
  val TextJavaScript: ContentType  = "text/javascript"
  val TextCsv: ContentType         = "text/csv"
  val TextXml: ContentType         = "text/xml"
  val TextEventStream: ContentType = "text/event-stream"

  // Multipart types
  val MultipartFormData: ContentType = "multipart/form-data"
  val MultipartMixed: ContentType    = "multipart/mixed"

  // Image types
  val ImagePng: ContentType  = "image/png"
  val ImageJpeg: ContentType = "image/jpeg"
  val ImageGif: ContentType  = "image/gif"
  val ImageWebp: ContentType = "image/webp"
  val ImageSvg: ContentType  = "image/svg+xml"

  // Audio types
  val AudioMpeg: ContentType = "audio/mpeg"
  val AudioOgg: ContentType  = "audio/ogg"
  val AudioWav: ContentType  = "audio/wav"

  // Video types
  val VideoMp4: ContentType  = "video/mp4"
  val VideoWebm: ContentType = "video/webm"

  def parse(value: String): Option[ContentType] =
    if value.isEmpty then None
    else Some(value)

  extension (ct: ContentType)
    def value: String = ct

    def mediaType: String =
      val idx = ct.indexOf('/')
      if idx >= 0 then ct.substring(0, idx).toLowerCase else ct.toLowerCase

    def subType: String =
      val slashIdx     = ct.indexOf('/')
      val semicolonIdx = ct.indexOf(';')
      if slashIdx < 0 then ""
      else if semicolonIdx < 0 then ct.substring(slashIdx + 1).trim.toLowerCase
      else ct.substring(slashIdx + 1, semicolonIdx).trim.toLowerCase

    def fullType: String =
      val semicolonIdx = ct.indexOf(';')
      val base = if semicolonIdx < 0 then ct else ct.substring(0, semicolonIdx)
      base.trim.toLowerCase

    def charset: Option[String] =
      val lower = ct.toLowerCase
      val idx   = lower.indexOf("charset=")
      if idx < 0 then None
      else
        val start = idx + 8
        val end = ct.indexOf(';', start) match
          case -1 => ct.length
          case n  => n
        Some(ct.substring(start, end).trim.stripPrefix("\"").stripSuffix("\""))

    def withCharset(cs: String): ContentType = s"${fullType}; charset=${cs}"

    def isJson: Boolean = fullType == "application/json"
    def isXml: Boolean  = fullType == "application/xml" || fullType == "text/xml"
```

### 4. HttpHeader and HttpHeaders

```scala
package wvlet.uni.http

case class HttpHeader(name: String, value: String):
  def nameEquals(other: String): Boolean =
    name.equalsIgnoreCase(other)

object HttpHeader:
  // Standard header names
  val Accept: String             = "Accept"
  val AcceptCharset: String      = "Accept-Charset"
  val AcceptEncoding: String     = "Accept-Encoding"
  val AcceptLanguage: String     = "Accept-Language"
  val Authorization: String      = "Authorization"
  val CacheControl: String       = "Cache-Control"
  val Connection: String         = "Connection"
  val ContentDisposition: String = "Content-Disposition"
  val ContentEncoding: String    = "Content-Encoding"
  val ContentLength: String      = "Content-Length"
  val ContentType: String        = "Content-Type"
  val Cookie: String             = "Cookie"
  val Date: String               = "Date"
  val ETag: String               = "ETag"
  val Expect: String             = "Expect"
  val Expires: String            = "Expires"
  val Host: String               = "Host"
  val IfMatch: String            = "If-Match"
  val IfModifiedSince: String    = "If-Modified-Since"
  val IfNoneMatch: String        = "If-None-Match"
  val IfUnmodifiedSince: String  = "If-Unmodified-Since"
  val LastModified: String       = "Last-Modified"
  val Location: String           = "Location"
  val Origin: String             = "Origin"
  val Pragma: String             = "Pragma"
  val ProxyAuthorization: String = "Proxy-Authorization"
  val Range: String              = "Range"
  val Referer: String            = "Referer"
  val RetryAfter: String         = "Retry-After"
  val Server: String             = "Server"
  val SetCookie: String          = "Set-Cookie"
  val TransferEncoding: String   = "Transfer-Encoding"
  val UserAgent: String          = "User-Agent"
  val Vary: String               = "Vary"
  val WWWAuthenticate: String    = "WWW-Authenticate"
  val XForwardedFor: String      = "X-Forwarded-For"
  val XForwardedHost: String     = "X-Forwarded-Host"
  val XForwardedProto: String    = "X-Forwarded-Proto"
  val XRequestId: String         = "X-Request-Id"

case class HttpHeaders(entries: Seq[HttpHeader]):
  def get(name: String): Option[String] =
    entries.find(_.nameEquals(name)).map(_.value)

  def getAll(name: String): Seq[String] =
    entries.filter(_.nameEquals(name)).map(_.value)

  def contains(name: String): Boolean =
    entries.exists(_.nameEquals(name))

  def add(name: String, value: String): HttpHeaders =
    HttpHeaders(entries :+ HttpHeader(name, value))

  def set(name: String, value: String): HttpHeaders =
    HttpHeaders(entries.filterNot(_.nameEquals(name)) :+ HttpHeader(name, value))

  def remove(name: String): HttpHeaders =
    HttpHeaders(entries.filterNot(_.nameEquals(name)))

  def contentType: Option[ContentType] =
    get(HttpHeader.ContentType).flatMap(ContentType.parse)

  def contentLength: Option[Long] =
    get(HttpHeader.ContentLength).flatMap(_.toLongOption)

  def isEmpty: Boolean = entries.isEmpty
  def nonEmpty: Boolean = entries.nonEmpty
  def size: Int = entries.size

  def toMap: Map[String, String] =
    entries.map(h => h.name -> h.value).toMap

  def toMultiMap: Map[String, Seq[String]] =
    entries.groupBy(_.name.toLowerCase).map { case (k, vs) => k -> vs.map(_.value) }

object HttpHeaders:
  val empty: HttpHeaders = HttpHeaders(Seq.empty)

  def apply(headers: (String, String)*): HttpHeaders =
    HttpHeaders(headers.map { case (n, v) => HttpHeader(n, v) })

  def fromMap(map: Map[String, String]): HttpHeaders =
    HttpHeaders(map.toSeq.map { case (n, v) => HttpHeader(n, v) })
```

### 5. HttpContent

```scala
package wvlet.uni.http

import wvlet.uni.json.JSONValue

sealed trait HttpContent:
  def isEmpty: Boolean
  def nonEmpty: Boolean = !isEmpty
  def contentType: Option[ContentType]
  def length: Long

object HttpContent:
  case object Empty extends HttpContent:
    def isEmpty: Boolean = true
    def contentType: Option[ContentType] = None
    def length: Long = 0

  case class TextContent(
      text: String,
      override val contentType: Option[ContentType] = Some(ContentType.TextPlain)
  ) extends HttpContent:
    def isEmpty: Boolean = text.isEmpty
    def length: Long = text.getBytes("UTF-8").length

  case class ByteContent(
      bytes: Array[Byte],
      override val contentType: Option[ContentType] = Some(ContentType.ApplicationOctetStream)
  ) extends HttpContent:
    def isEmpty: Boolean = bytes.isEmpty
    def length: Long = bytes.length

  case class JsonContent(
      json: JSONValue,
      override val contentType: Option[ContentType] = Some(ContentType.ApplicationJson)
  ) extends HttpContent:
    def isEmpty: Boolean = false
    def length: Long = json.toJSON.getBytes("UTF-8").length

  def empty: HttpContent = Empty
  def text(s: String): HttpContent = TextContent(s)
  def bytes(b: Array[Byte]): HttpContent = ByteContent(b)
  def json(j: JSONValue): HttpContent = JsonContent(j)
```

### 6. Request

```scala
package wvlet.uni.http

import wvlet.uni.util.URLEncoder

case class Request(
    method: HttpMethod,
    uri: String,
    headers: HttpHeaders = HttpHeaders.empty,
    content: HttpContent = HttpContent.Empty,
    queryParams: Map[String, Seq[String]] = Map.empty  // Multi-value support
):
  def path: String =
    val idx = uri.indexOf('?')
    if idx >= 0 then uri.substring(0, idx) else uri

  def host: Option[String] = headers.get(HttpHeader.Host)

  def contentType: Option[ContentType] =
    content.contentType.orElse(headers.contentType)

  def getQueryParam(name: String): Option[String] =
    queryParams.get(name).flatMap(_.headOption)

  def getQueryParams(name: String): Seq[String] =
    queryParams.getOrElse(name, Seq.empty)

  def fullUri: String =
    if queryParams.isEmpty then uri
    else
      val queryString = queryParams
        .flatMap { case (k, vs) =>
          vs.map(v => s"${URLEncoder.encode(k)}=${URLEncoder.encode(v)}")
        }
        .mkString("&")
      val separator = if uri.contains("?") then "&" else "?"
      s"${uri}${separator}${queryString}"

  // Builder methods
  def withMethod(m: HttpMethod): Request = copy(method = m)
  def withUri(u: String): Request = copy(uri = u)
  def withHeaders(h: HttpHeaders): Request = copy(headers = h)
  def withContent(c: HttpContent): Request = copy(content = c)
  def withQueryParams(params: Map[String, Seq[String]]): Request = copy(queryParams = params)

  def addHeader(name: String, value: String): Request =
    copy(headers = headers.add(name, value))
  def setHeader(name: String, value: String): Request =
    copy(headers = headers.set(name, value))

  def addQueryParam(name: String, value: String): Request =
    val existing = queryParams.getOrElse(name, Seq.empty)
    copy(queryParams = queryParams + (name -> (existing :+ value)))

  def setQueryParam(name: String, value: String): Request =
    copy(queryParams = queryParams + (name -> Seq(value)))

  def withTextContent(text: String): Request =
    copy(content = HttpContent.text(text))
  def withJsonContent(json: JSONValue): Request =
    copy(content = HttpContent.json(json))
  def withBytesContent(bytes: Array[Byte]): Request =
    copy(content = HttpContent.bytes(bytes))

object Request:
  def get(uri: String): Request = Request(HttpMethod.GET, uri)
  def post(uri: String): Request = Request(HttpMethod.POST, uri)
  def put(uri: String): Request = Request(HttpMethod.PUT, uri)
  def delete(uri: String): Request = Request(HttpMethod.DELETE, uri)
  def patch(uri: String): Request = Request(HttpMethod.PATCH, uri)
  def head(uri: String): Request = Request(HttpMethod.HEAD, uri)
  def options(uri: String): Request = Request(HttpMethod.OPTIONS, uri)

// Type aliases in package object for backwards compatibility:
package object http:
  type HttpRequest = Request
  val HttpRequest = Request
```

### 7. Response

```scala
package wvlet.uni.http

import wvlet.uni.json.JSON
import wvlet.uni.json.JSON.JSONValue

case class Response(
    status: HttpStatus,
    headers: HttpHeaders = HttpHeaders.empty,
    content: HttpContent = HttpContent.Empty
):
  def isSuccessful: Boolean = status.isSuccessful
  def isClientError: Boolean = status.isClientError
  def isServerError: Boolean = status.isServerError
  def isRedirection: Boolean = status.isRedirection

  def statusCode: Int = status.code

  def contentType: Option[ContentType] =
    content.contentType.orElse(headers.contentType)

  def contentLength: Option[Long] =
    headers.contentLength

  def contentAsString: Option[String] = content.asString

  def contentAsBytes: Option[Array[Byte]] = content.asBytes

  def contentAsJson: Option[JSONValue] =
    content match
      case HttpContent.JsonContent(json, _) => Some(json)
      case other =>
        other.asString.flatMap { str =>
          try Some(JSON.parse(str))
          catch case _: Exception => None  // Returns None on parse failure
        }

  def location: Option[String] = headers.get(HttpHeader.Location)

  // Builder methods
  def withStatus(s: HttpStatus): Response = copy(status = s)
  def withHeaders(h: HttpHeaders): Response = copy(headers = h)
  def withContent(c: HttpContent): Response = copy(content = c)

  def addHeader(name: String, value: String): Response =
    copy(headers = headers.add(name, value))
  def setHeader(name: String, value: String): Response =
    copy(headers = headers.set(name, value))

object Response:
  def apply(status: HttpStatus): Response =
    Response(status, HttpHeaders.empty, HttpContent.Empty)

  def ok: Response = Response(HttpStatus.Ok_200)
  def created: Response = Response(HttpStatus.Created_201)
  def noContent: Response = Response(HttpStatus.NoContent_204)
  def redirect(location: String): Response = Response(HttpStatus.Found_302).withLocation(location)
  def badRequest: Response = Response(HttpStatus.BadRequest_400)
  def unauthorized: Response = Response(HttpStatus.Unauthorized_401)
  def forbidden: Response = Response(HttpStatus.Forbidden_403)
  def notFound: Response = Response(HttpStatus.NotFound_404)
  def internalServerError: Response = Response(HttpStatus.InternalServerError_500)

// Type aliases in package object for backwards compatibility:
package object http:
  type HttpResponse = Response
  val HttpResponse = Response
```

## Client Interfaces

### 8. HttpClientConfig

Uses `Retry.RetryContext` from `wvlet.uni.control` for retry logic, avoiding duplication.

```scala
package wvlet.uni.http

import wvlet.uni.control.Retry
import wvlet.uni.control.Retry.RetryContext

case class HttpClientConfig(
    baseUri: Option[String] = None,
    connectTimeoutMillis: Long = 30000,
    readTimeoutMillis: Long = 60000,
    followRedirects: Boolean = true,
    maxRedirects: Int = 10,
    retryContext: RetryContext = Retry
      .withBackOff(maxRetry = 3, initialIntervalMillis = 1000, maxIntervalMillis = 30000)
      .noRetryLogging,
    requestFilter: HttpRequest => HttpRequest = identity,
    channelFactory: HttpChannelFactory = HttpClientConfig.defaultChannelFactory
):
  def withBaseUri(uri: String): HttpClientConfig = copy(baseUri = Some(uri))
  def noBaseUri: HttpClientConfig = copy(baseUri = None)

  def withConnectTimeoutMillis(millis: Long): HttpClientConfig = copy(connectTimeoutMillis = millis)
  def withReadTimeoutMillis(millis: Long): HttpClientConfig = copy(readTimeoutMillis = millis)

  def withFollowRedirects: HttpClientConfig = copy(followRedirects = true)
  def noFollowRedirects: HttpClientConfig = copy(followRedirects = false)

  def withMaxRedirects(max: Int): HttpClientConfig = copy(maxRedirects = max)

  def withRetryContext(ctx: RetryContext): HttpClientConfig = copy(retryContext = ctx)
  def withMaxRetry(maxRetries: Int): HttpClientConfig =
    copy(retryContext = retryContext.withMaxRetry(maxRetries))
  def noRetry: HttpClientConfig = copy(retryContext = retryContext.withMaxRetry(0))

  def withRequestFilter(filter: HttpRequest => HttpRequest): HttpClientConfig =
    copy(requestFilter = filter)
  def addRequestFilter(filter: HttpRequest => HttpRequest): HttpClientConfig =
    copy(requestFilter = requestFilter.andThen(filter))

  def resolveUri(uri: String): String =
    baseUri.fold(uri)(base => s"${base}${uri}")

object HttpClientConfig:
  val default: HttpClientConfig = HttpClientConfig()

  // Platform-specific channel factory (set by JVM/JS/Native implementations)
  var defaultChannelFactory: HttpChannelFactory = NoOpChannelFactory

  object NoOpChannelFactory extends HttpChannelFactory:
    def createChannel(config: HttpClientConfig): HttpChannel = ???
    def createAsyncChannel(config: HttpClientConfig): HttpAsyncChannel = ???
```

### 9. HttpClient (Sync)

```scala
package wvlet.uni.http

trait HttpClient extends AutoCloseable:
  def config: HttpClientConfig

  def send(request: HttpRequest): HttpResponse

  // Convenience methods
  def get(uri: String, headers: HttpHeaders = HttpHeaders.empty): HttpResponse =
    send(HttpRequest.get(uri).withHeaders(headers))

  def post(uri: String, content: HttpContent, headers: HttpHeaders = HttpHeaders.empty): HttpResponse =
    send(HttpRequest.post(uri).withContent(content).withHeaders(headers))

  def put(uri: String, content: HttpContent, headers: HttpHeaders = HttpHeaders.empty): HttpResponse =
    send(HttpRequest.put(uri).withContent(content).withHeaders(headers))

  def delete(uri: String, headers: HttpHeaders = HttpHeaders.empty): HttpResponse =
    send(HttpRequest.delete(uri).withHeaders(headers))

  def patch(uri: String, content: HttpContent, headers: HttpHeaders = HttpHeaders.empty): HttpResponse =
    send(HttpRequest.patch(uri).withContent(content).withHeaders(headers))

  def head(uri: String, headers: HttpHeaders = HttpHeaders.empty): HttpResponse =
    send(HttpRequest.head(uri).withHeaders(headers))

object HttpClient:
  // Platform-specific factory will be provided in .jvm/.js/.native
```

### 10. AsyncHttpClient

```scala
package wvlet.uni.http

import scala.concurrent.Future

trait AsyncHttpClient extends AutoCloseable:
  def config: HttpClientConfig

  def sendAsync(request: HttpRequest): Future[HttpResponse]

  // Convenience methods
  def getAsync(uri: String, headers: HttpHeaders = HttpHeaders.empty): Future[HttpResponse] =
    sendAsync(HttpRequest.get(uri).withHeaders(headers))

  def postAsync(uri: String, content: HttpContent, headers: HttpHeaders = HttpHeaders.empty): Future[HttpResponse] =
    sendAsync(HttpRequest.post(uri).withContent(content).withHeaders(headers))

  def putAsync(uri: String, content: HttpContent, headers: HttpHeaders = HttpHeaders.empty): Future[HttpResponse] =
    sendAsync(HttpRequest.put(uri).withContent(content).withHeaders(headers))

  def deleteAsync(uri: String, headers: HttpHeaders = HttpHeaders.empty): Future[HttpResponse] =
    sendAsync(HttpRequest.delete(uri).withHeaders(headers))

  def patchAsync(uri: String, content: HttpContent, headers: HttpHeaders = HttpHeaders.empty): Future[HttpResponse] =
    sendAsync(HttpRequest.patch(uri).withContent(content).withHeaders(headers))

trait HttpResponseObserver:
  def onHeaders(response: HttpResponse): Unit
  def onContent(chunk: Array[Byte]): Unit
  def onComplete(): Unit
  def onError(e: Throwable): Unit

trait StreamingHttpClient extends AsyncHttpClient:
  def sendStreaming(request: HttpRequest, observer: HttpResponseObserver): Unit
```

## Server Interfaces

### 11. HttpHandler and HttpFilter

```scala
package wvlet.uni.http

trait HttpHandler:
  def handle(request: HttpRequest): HttpResponse

trait HttpFilter:
  def apply(request: HttpRequest, next: HttpHandler): HttpResponse

object HttpFilter:
  val identity: HttpFilter = (request, next) => next.handle(request)

  def chain(filters: Seq[HttpFilter]): HttpFilter =
    filters.foldRight(identity) { (filter, acc) =>
      (request, next) => filter.apply(request, (req) => acc.apply(req, next))
    }
```

## Exception Hierarchy

### 12. HttpException

Instead of a deep exception hierarchy, we use a single `HttpException` class with an `HttpErrorCode` enum to categorize errors. This simplifies error handling while still providing detailed error information.

```scala
package wvlet.uni.http

enum HttpErrorCode:
  // Connection errors
  case ConnectionFailed
  case ConnectionTimeout
  case ReadTimeout
  case SslError

  // Request errors
  case InvalidRequest
  case RequestCancelled
  case TooManyRedirects

  // Response errors
  case ClientError  // 4xx
  case ServerError  // 5xx
  case InvalidResponse
  case UnexpectedResponse

  // Other errors
  case Unknown

  def isRetryable: Boolean = this match
    case ConnectionTimeout | ReadTimeout | ServerError => true
    case _                                             => false

class HttpException(
    message: String,
    val errorCode: HttpErrorCode = HttpErrorCode.Unknown,
    val status: Option[HttpStatus] = None,
    val request: Option[HttpRequest] = None,
    val response: Option[HttpResponse] = None,
    cause: Throwable = null
) extends Exception(message, cause):

  def statusCode: Option[Int] = status.map(_.code)

  def isRetryable: Boolean = errorCode.isRetryable || status.exists(_.isRetryable)

  def isClientError: Boolean = errorCode == HttpErrorCode.ClientError || status.exists(_.isClientError)
  def isServerError: Boolean = errorCode == HttpErrorCode.ServerError || status.exists(_.isServerError)

object HttpException:
  def apply(message: String): HttpException =
    HttpException(message, HttpErrorCode.Unknown)

  def apply(message: String, errorCode: HttpErrorCode): HttpException =
    HttpException(message, errorCode, None, None, None, null)

  def apply(message: String, status: HttpStatus): HttpException =
    val errorCode = if status.isClientError then HttpErrorCode.ClientError
                    else if status.isServerError then HttpErrorCode.ServerError
                    else HttpErrorCode.Unknown
    HttpException(message, errorCode, Some(status), None, None, null)

  def fromResponse(response: HttpResponse): HttpException =
    val message   = response.contentAsString.getOrElse(response.status.reason)
    val errorCode = if response.isClientError then HttpErrorCode.ClientError
                    else if response.isServerError then HttpErrorCode.ServerError
                    else HttpErrorCode.UnexpectedResponse
    HttpException(message, errorCode, Some(response.status), None, Some(response), null)

  // Factory methods for common error codes
  def connectionFailed(message: String, cause: Throwable = null): HttpException =
    HttpException(message, HttpErrorCode.ConnectionFailed, None, None, None, cause)

  def connectionTimeout(message: String, cause: Throwable = null): HttpException =
    HttpException(message, HttpErrorCode.ConnectionTimeout, None, None, None, cause)

  def readTimeout(message: String, cause: Throwable = null): HttpException =
    HttpException(message, HttpErrorCode.ReadTimeout, None, None, None, cause)

  def clientError(message: String, status: HttpStatus, response: Option[HttpResponse] = None): HttpException =
    HttpException(message, HttpErrorCode.ClientError, Some(status), None, response, null)

  def serverError(message: String, status: HttpStatus, response: Option[HttpResponse] = None): HttpException =
    HttpException(message, HttpErrorCode.ServerError, Some(status), None, response, null)
```

## Usage Examples

### Basic GET Request

```scala
import wvlet.uni.http.*

val client = HttpClient.create()
val response = client.get("https://api.example.com/users")

if response.isSuccessful then
  println(response.contentAsString)
else
  println(s"Error: ${response.status}")
```

### POST with JSON

```scala
import wvlet.uni.http.*
import wvlet.uni.json.*

val client = HttpClient.create(
  HttpClientConfig.default
    .withBaseUri("https://api.example.com")
    .withUserAgent("MyApp/1.0")
)

val json = JSON.parse("""{"name": "John", "email": "john@example.com"}""")
val response = client.post("/users", HttpContent.json(json))
```

### Async Client

```scala
import wvlet.uni.http.*
import scala.concurrent.ExecutionContext.Implicits.global

val asyncClient = AsyncHttpClient.create()
val futureResponse = asyncClient.getAsync("https://api.example.com/data")

futureResponse.foreach { response =>
  println(s"Status: ${response.status}")
}
```

### Error Handling

```scala
import wvlet.uni.http.*

try
  val response = client.get("/resource")
  // process response
catch
  case e: HttpException if e.errorCode == HttpErrorCode.ConnectionTimeout =>
    println("Request timed out, retrying...")
  case e: HttpException if e.status.exists(_.code == 404) =>
    println("Resource not found")
  case e: HttpException if e.isServerError =>
    if e.isRetryable then
      println("Server error, will retry")
```

## Future Considerations

1. **Netty Integration**: The abstractions support building a Netty-based HTTP server
2. **RPC Layer**: RPCStatus can map to HttpStatus for gRPC/HTTP interop
3. **Streaming**: HttpResponseObserver enables streaming responses
4. **Middleware**: HttpFilter chain for logging, auth, metrics
5. **Connection Pooling**: Platform-specific client implementations can manage pools
6. **HTTP/2 Support**: Future enhancement for modern HTTP features
7. **WebSocket**: Can extend with WebSocket upgrade support

## References

- [RFC 9110 - HTTP Semantics](https://httpwg.org/specs/rfc9110.html)
- [IANA HTTP Status Code Registry](https://www.iana.org/assignments/http-status-codes)
- [IANA Media Types Registry](https://www.iana.org/assignments/media-types/)
- [Python requests](https://docs.python-requests.org/)
- [Rust reqwest](https://github.com/seanmonstar/reqwest)
- [Go net/http](https://pkg.go.dev/net/http)
- [airframe-http](https://wvlet.org/airframe/docs/airframe-http)
