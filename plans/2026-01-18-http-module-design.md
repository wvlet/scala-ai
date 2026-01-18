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
  // ... more status codes

  // Unknown status holder for non-standard codes
  case Unknown(override val code: Int) extends HttpStatus(code, "Unknown")

  def isInformational: Boolean = code >= 100 && code < 200
  def isSuccessful: Boolean    = code >= 200 && code < 300
  def isRedirection: Boolean   = code >= 300 && code < 400
  def isClientError: Boolean   = code >= 400 && code < 500
  def isServerError: Boolean   = code >= 500 && code < 600
  def isRetryable: Boolean     = // ServiceUnavailable, GatewayTimeout, TooManyRequests, RequestTimeout

object HttpStatus:
  def ofCode(code: Int): HttpStatus = // lookup or Unknown(code)

  // 2xx Success
  case object Ok_200                  extends HttpStatus(200, "OK")
  case object Created_201             extends HttpStatus(201, "Created")
  case object Accepted_202            extends HttpStatus(202, "Accepted")
  case object NonAuthoritativeInfo_203 extends HttpStatus(203, "Non-Authoritative Information")
  case object NoContent_204           extends HttpStatus(204, "No Content")
  case object ResetContent_205        extends HttpStatus(205, "Reset Content")
  case object PartialContent_206      extends HttpStatus(206, "Partial Content")
  case object MultiStatus_207         extends HttpStatus(207, "Multi-Status")
  case object AlreadyReported_208     extends HttpStatus(208, "Already Reported")
  case object IMUsed_226              extends HttpStatus(226, "IM Used")

  // 3xx Redirection
  case object MultipleChoices_300     extends HttpStatus(300, "Multiple Choices")
  case object MovedPermanently_301    extends HttpStatus(301, "Moved Permanently")
  case object Found_302               extends HttpStatus(302, "Found")
  case object SeeOther_303            extends HttpStatus(303, "See Other")
  case object NotModified_304         extends HttpStatus(304, "Not Modified")
  case object UseProxy_305            extends HttpStatus(305, "Use Proxy")
  case object TemporaryRedirect_307   extends HttpStatus(307, "Temporary Redirect")
  case object PermanentRedirect_308   extends HttpStatus(308, "Permanent Redirect")

  // 4xx Client Error
  case object BadRequest_400          extends HttpStatus(400, "Bad Request")
  case object Unauthorized_401        extends HttpStatus(401, "Unauthorized")
  case object PaymentRequired_402     extends HttpStatus(402, "Payment Required")
  case object Forbidden_403           extends HttpStatus(403, "Forbidden")
  case object NotFound_404            extends HttpStatus(404, "Not Found")
  case object MethodNotAllowed_405    extends HttpStatus(405, "Method Not Allowed")
  case object NotAcceptable_406       extends HttpStatus(406, "Not Acceptable")
  case object ProxyAuthRequired_407   extends HttpStatus(407, "Proxy Authentication Required")
  case object RequestTimeout_408      extends HttpStatus(408, "Request Timeout")
  case object Conflict_409            extends HttpStatus(409, "Conflict")
  case object Gone_410                extends HttpStatus(410, "Gone")
  case object LengthRequired_411      extends HttpStatus(411, "Length Required")
  case object PreconditionFailed_412  extends HttpStatus(412, "Precondition Failed")
  case object PayloadTooLarge_413     extends HttpStatus(413, "Payload Too Large")
  case object UriTooLong_414          extends HttpStatus(414, "URI Too Long")
  case object UnsupportedMediaType_415 extends HttpStatus(415, "Unsupported Media Type")
  case object RangeNotSatisfiable_416 extends HttpStatus(416, "Range Not Satisfiable")
  case object ExpectationFailed_417   extends HttpStatus(417, "Expectation Failed")
  case object ImATeapot_418           extends HttpStatus(418, "I'm a teapot")
  case object MisdirectedRequest_421  extends HttpStatus(421, "Misdirected Request")
  case object UnprocessableEntity_422 extends HttpStatus(422, "Unprocessable Entity")
  case object Locked_423              extends HttpStatus(423, "Locked")
  case object FailedDependency_424    extends HttpStatus(424, "Failed Dependency")
  case object TooEarly_425            extends HttpStatus(425, "Too Early")
  case object UpgradeRequired_426     extends HttpStatus(426, "Upgrade Required")
  case object PreconditionRequired_428 extends HttpStatus(428, "Precondition Required")
  case object TooManyRequests_429     extends HttpStatus(429, "Too Many Requests")
  case object HeaderFieldsTooLarge_431 extends HttpStatus(431, "Request Header Fields Too Large")
  case object UnavailableForLegal_451 extends HttpStatus(451, "Unavailable For Legal Reasons")
  case object ClientClosedRequest_499 extends HttpStatus(499, "Client Closed Request")

  // 5xx Server Error
  case object InternalServerError_500 extends HttpStatus(500, "Internal Server Error")
  case object NotImplemented_501      extends HttpStatus(501, "Not Implemented")
  case object BadGateway_502          extends HttpStatus(502, "Bad Gateway")
  case object ServiceUnavailable_503  extends HttpStatus(503, "Service Unavailable")
  case object GatewayTimeout_504      extends HttpStatus(504, "Gateway Timeout")
  case object HttpVersionNotSupported_505 extends HttpStatus(505, "HTTP Version Not Supported")
  case object VariantAlsoNegotiates_506 extends HttpStatus(506, "Variant Also Negotiates")
  case object InsufficientStorage_507 extends HttpStatus(507, "Insufficient Storage")
  case object LoopDetected_508        extends HttpStatus(508, "Loop Detected")
  case object NotExtended_510         extends HttpStatus(510, "Not Extended")
  case object NetworkAuthRequired_511 extends HttpStatus(511, "Network Authentication Required")

  // Unknown status holder
  case class Unknown(override val code: Int) extends HttpStatus(code, "Unknown")

  private val statusByCode: Map[Int, HttpStatus] =
    values.collect { case s: HttpStatus => s.code -> s }.toMap

  def ofCode(code: Int): HttpStatus =
    statusByCode.getOrElse(code, Unknown(code))

  def values: Seq[HttpStatus] = Seq(
    Continue_100, SwitchingProtocols_101, Processing_102, EarlyHints_103,
    Ok_200, Created_201, Accepted_202, NonAuthoritativeInfo_203, NoContent_204,
    ResetContent_205, PartialContent_206, MultiStatus_207, AlreadyReported_208, IMUsed_226,
    MultipleChoices_300, MovedPermanently_301, Found_302, SeeOther_303, NotModified_304,
    UseProxy_305, TemporaryRedirect_307, PermanentRedirect_308,
    BadRequest_400, Unauthorized_401, PaymentRequired_402, Forbidden_403, NotFound_404,
    MethodNotAllowed_405, NotAcceptable_406, ProxyAuthRequired_407, RequestTimeout_408,
    Conflict_409, Gone_410, LengthRequired_411, PreconditionFailed_412, PayloadTooLarge_413,
    UriTooLong_414, UnsupportedMediaType_415, RangeNotSatisfiable_416, ExpectationFailed_417,
    ImATeapot_418, MisdirectedRequest_421, UnprocessableEntity_422, Locked_423,
    FailedDependency_424, TooEarly_425, UpgradeRequired_426, PreconditionRequired_428,
    TooManyRequests_429, HeaderFieldsTooLarge_431, UnavailableForLegal_451, ClientClosedRequest_499,
    InternalServerError_500, NotImplemented_501, BadGateway_502, ServiceUnavailable_503,
    GatewayTimeout_504, HttpVersionNotSupported_505, VariantAlsoNegotiates_506,
    InsufficientStorage_507, LoopDetected_508, NotExtended_510, NetworkAuthRequired_511
  )
```

### 3. ContentType

```scala
package wvlet.uni.http

case class ContentType(
    mediaType: String,
    subType: String,
    charset: Option[String] = None,
    parameters: Map[String, String] = Map.empty
):
  def fullType: String = s"${mediaType}/${subType}"

  def withCharset(cs: String): ContentType = copy(charset = Some(cs))
  def noCharset: ContentType = copy(charset = None)

  def withParameter(key: String, value: String): ContentType =
    copy(parameters = parameters + (key -> value))

  override def toString: String =
    val base = fullType
    val charsetPart = charset.map(c => s"; charset=${c}").getOrElse("")
    val paramsPart = parameters.map { case (k, v) => s"; ${k}=${v}" }.mkString
    base + charsetPart + paramsPart

object ContentType:
  // Application types
  val ApplicationJson: ContentType       = ContentType("application", "json")
  val ApplicationXml: ContentType        = ContentType("application", "xml")
  val ApplicationFormUrlEncoded: ContentType = ContentType("application", "x-www-form-urlencoded")
  val ApplicationOctetStream: ContentType = ContentType("application", "octet-stream")
  val ApplicationMsgPack: ContentType    = ContentType("application", "msgpack")
  val ApplicationPdf: ContentType        = ContentType("application", "pdf")
  val ApplicationZip: ContentType        = ContentType("application", "zip")
  val ApplicationGzip: ContentType       = ContentType("application", "gzip")

  // Text types
  val TextPlain: ContentType             = ContentType("text", "plain")
  val TextHtml: ContentType              = ContentType("text", "html")
  val TextCss: ContentType               = ContentType("text", "css")
  val TextJavaScript: ContentType        = ContentType("text", "javascript")
  val TextCsv: ContentType               = ContentType("text", "csv")
  val TextXml: ContentType               = ContentType("text", "xml")

  // Multipart types
  val MultipartFormData: ContentType     = ContentType("multipart", "form-data")
  val MultipartMixed: ContentType        = ContentType("multipart", "mixed")

  // Image types
  val ImagePng: ContentType              = ContentType("image", "png")
  val ImageJpeg: ContentType             = ContentType("image", "jpeg")
  val ImageGif: ContentType              = ContentType("image", "gif")
  val ImageWebp: ContentType             = ContentType("image", "webp")
  val ImageSvg: ContentType              = ContentType("image", "svg+xml")

  // Audio types
  val AudioMpeg: ContentType             = ContentType("audio", "mpeg")
  val AudioOgg: ContentType              = ContentType("audio", "ogg")
  val AudioWav: ContentType              = ContentType("audio", "wav")

  // Video types
  val VideoMp4: ContentType              = ContentType("video", "mp4")
  val VideoWebm: ContentType             = ContentType("video", "webm")

  def parse(value: String): Option[ContentType] =
    value.split(";").toList match
      case typePart :: rest =>
        typePart.trim.split("/").toList match
          case mediaType :: subType :: Nil =>
            val params = rest.map(_.trim).flatMap { p =>
              p.split("=", 2).toList match
                case k :: v :: Nil => Some(k.trim.toLowerCase -> v.trim)
                case _             => None
            }.toMap
            val charset = params.get("charset")
            Some(ContentType(mediaType.trim, subType.trim, charset, params - "charset"))
          case _ => None
      case _ => None
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

### 6. HttpRequest

```scala
package wvlet.uni.http

import java.net.URI

case class HttpRequest(
    method: HttpMethod,
    uri: String,
    headers: HttpHeaders = HttpHeaders.empty,
    content: HttpContent = HttpContent.Empty,
    queryParams: Map[String, String] = Map.empty
):
  def path: String =
    val idx = uri.indexOf('?')
    if idx >= 0 then uri.substring(0, idx) else uri

  def host: Option[String] = headers.get(HttpHeader.Host)

  def contentType: Option[ContentType] =
    content.contentType.orElse(headers.contentType)

  // Builder methods
  def withMethod(m: HttpMethod): HttpRequest = copy(method = m)
  def withUri(u: String): HttpRequest = copy(uri = u)
  def withHeaders(h: HttpHeaders): HttpRequest = copy(headers = h)
  def withContent(c: HttpContent): HttpRequest = copy(content = c)
  def withQueryParams(params: Map[String, String]): HttpRequest = copy(queryParams = params)

  def addHeader(name: String, value: String): HttpRequest =
    copy(headers = headers.add(name, value))
  def setHeader(name: String, value: String): HttpRequest =
    copy(headers = headers.set(name, value))

  def addQueryParam(name: String, value: String): HttpRequest =
    copy(queryParams = queryParams + (name -> value))

  def withTextContent(text: String): HttpRequest =
    copy(content = HttpContent.text(text))
  def withJsonContent(json: JSONValue): HttpRequest =
    copy(content = HttpContent.json(json))
  def withBytesContent(bytes: Array[Byte]): HttpRequest =
    copy(content = HttpContent.bytes(bytes))

object HttpRequest:
  def get(uri: String): HttpRequest =
    HttpRequest(HttpMethod.GET, uri)
  def post(uri: String): HttpRequest =
    HttpRequest(HttpMethod.POST, uri)
  def put(uri: String): HttpRequest =
    HttpRequest(HttpMethod.PUT, uri)
  def delete(uri: String): HttpRequest =
    HttpRequest(HttpMethod.DELETE, uri)
  def patch(uri: String): HttpRequest =
    HttpRequest(HttpMethod.PATCH, uri)
  def head(uri: String): HttpRequest =
    HttpRequest(HttpMethod.HEAD, uri)
  def options(uri: String): HttpRequest =
    HttpRequest(HttpMethod.OPTIONS, uri)
```

### 7. HttpResponse

```scala
package wvlet.uni.http

case class HttpResponse(
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

  def contentAsString: Option[String] = content match
    case HttpContent.TextContent(text, _) => Some(text)
    case HttpContent.ByteContent(bytes, _) => Some(String(bytes, "UTF-8"))
    case HttpContent.JsonContent(json, _) => Some(json.toJSON)
    case HttpContent.Empty => None

  def contentAsBytes: Option[Array[Byte]] = content match
    case HttpContent.TextContent(text, _) => Some(text.getBytes("UTF-8"))
    case HttpContent.ByteContent(bytes, _) => Some(bytes)
    case HttpContent.JsonContent(json, _) => Some(json.toJSON.getBytes("UTF-8"))
    case HttpContent.Empty => None

  // Builder methods
  def withStatus(s: HttpStatus): HttpResponse = copy(status = s)
  def withHeaders(h: HttpHeaders): HttpResponse = copy(headers = h)
  def withContent(c: HttpContent): HttpResponse = copy(content = c)

  def addHeader(name: String, value: String): HttpResponse =
    copy(headers = headers.add(name, value))
  def setHeader(name: String, value: String): HttpResponse =
    copy(headers = headers.set(name, value))

object HttpResponse:
  def apply(status: HttpStatus): HttpResponse =
    HttpResponse(status, HttpHeaders.empty, HttpContent.Empty)

  def ok: HttpResponse = HttpResponse(HttpStatus.Ok_200)
  def created: HttpResponse = HttpResponse(HttpStatus.Created_201)
  def noContent: HttpResponse = HttpResponse(HttpStatus.NoContent_204)
  def badRequest: HttpResponse = HttpResponse(HttpStatus.BadRequest_400)
  def unauthorized: HttpResponse = HttpResponse(HttpStatus.Unauthorized_401)
  def forbidden: HttpResponse = HttpResponse(HttpStatus.Forbidden_403)
  def notFound: HttpResponse = HttpResponse(HttpStatus.NotFound_404)
  def internalServerError: HttpResponse = HttpResponse(HttpStatus.InternalServerError_500)
```

## Client Interfaces

### 8. HttpClientConfig

```scala
package wvlet.uni.http

import scala.concurrent.duration.*

case class HttpClientConfig(
    baseUri: Option[String] = None,
    connectTimeout: Duration = 30.seconds,
    readTimeout: Duration = 60.seconds,
    followRedirects: Boolean = true,
    maxRedirects: Int = 10,
    retryCount: Int = 3,
    retryWaitMillis: Long = 1000,
    defaultHeaders: HttpHeaders = HttpHeaders.empty,
    userAgent: Option[String] = None
):
  def withBaseUri(uri: String): HttpClientConfig = copy(baseUri = Some(uri))
  def noBaseUri: HttpClientConfig = copy(baseUri = None)

  def withConnectTimeout(t: Duration): HttpClientConfig = copy(connectTimeout = t)
  def withReadTimeout(t: Duration): HttpClientConfig = copy(readTimeout = t)

  def withFollowRedirects(follow: Boolean): HttpClientConfig = copy(followRedirects = follow)
  def noFollowRedirects: HttpClientConfig = copy(followRedirects = false)

  def withMaxRedirects(max: Int): HttpClientConfig = copy(maxRedirects = max)

  def withRetryCount(count: Int): HttpClientConfig = copy(retryCount = count)
  def noRetry: HttpClientConfig = copy(retryCount = 0)

  def withRetryWaitMillis(millis: Long): HttpClientConfig = copy(retryWaitMillis = millis)

  def withDefaultHeaders(h: HttpHeaders): HttpClientConfig = copy(defaultHeaders = h)
  def addDefaultHeader(name: String, value: String): HttpClientConfig =
    copy(defaultHeaders = defaultHeaders.add(name, value))

  def withUserAgent(ua: String): HttpClientConfig = copy(userAgent = Some(ua))
  def noUserAgent: HttpClientConfig = copy(userAgent = None)

object HttpClientConfig:
  val default: HttpClientConfig = HttpClientConfig()
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

```scala
package wvlet.uni.http

class HttpException(
    message: String,
    val status: Option[HttpStatus] = None,
    val request: Option[HttpRequest] = None,
    val response: Option[HttpResponse] = None,
    cause: Throwable = null
) extends Exception(message, cause):

  def statusCode: Option[Int] = status.map(_.code)

  def isRetryable: Boolean = status.exists(_.isRetryable)

object HttpException:
  def apply(message: String): HttpException =
    HttpException(message)
  def apply(message: String, status: HttpStatus): HttpException =
    HttpException(message, Some(status))
  def apply(message: String, response: HttpResponse): HttpException =
    HttpException(message, Some(response.status), None, Some(response))

// Connection-related exceptions
class HttpConnectionException(
    message: String,
    cause: Throwable = null
) extends HttpException(message, cause = cause)

class HttpTimeoutException(
    message: String,
    cause: Throwable = null
) extends HttpException(message, cause = cause)

class HttpConnectTimeoutException(
    message: String,
    cause: Throwable = null
) extends HttpTimeoutException(message, cause)

class HttpReadTimeoutException(
    message: String,
    cause: Throwable = null
) extends HttpTimeoutException(message, cause)

// Status-based exceptions
class HttpClientErrorException(
    message: String,
    override val status: Option[HttpStatus],
    override val response: Option[HttpResponse] = None,
    cause: Throwable = null
) extends HttpException(message, status, None, response, cause)

class HttpServerErrorException(
    message: String,
    override val status: Option[HttpStatus],
    override val response: Option[HttpResponse] = None,
    cause: Throwable = null
) extends HttpException(message, status, None, response, cause)

// Specific client errors
class HttpBadRequestException(message: String, response: Option[HttpResponse] = None)
  extends HttpClientErrorException(message, Some(HttpStatus.BadRequest_400), response)

class HttpUnauthorizedException(message: String, response: Option[HttpResponse] = None)
  extends HttpClientErrorException(message, Some(HttpStatus.Unauthorized_401), response)

class HttpForbiddenException(message: String, response: Option[HttpResponse] = None)
  extends HttpClientErrorException(message, Some(HttpStatus.Forbidden_403), response)

class HttpNotFoundException(message: String, response: Option[HttpResponse] = None)
  extends HttpClientErrorException(message, Some(HttpStatus.NotFound_404), response)

class HttpConflictException(message: String, response: Option[HttpResponse] = None)
  extends HttpClientErrorException(message, Some(HttpStatus.Conflict_409), response)

class HttpTooManyRequestsException(message: String, response: Option[HttpResponse] = None)
  extends HttpClientErrorException(message, Some(HttpStatus.TooManyRequests_429), response)

// Specific server errors
class HttpInternalServerErrorException(message: String, response: Option[HttpResponse] = None)
  extends HttpServerErrorException(message, Some(HttpStatus.InternalServerError_500), response)

class HttpServiceUnavailableException(message: String, response: Option[HttpResponse] = None)
  extends HttpServerErrorException(message, Some(HttpStatus.ServiceUnavailable_503), response)

class HttpGatewayTimeoutException(message: String, response: Option[HttpResponse] = None)
  extends HttpServerErrorException(message, Some(HttpStatus.GatewayTimeout_504), response)
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
  case e: HttpNotFoundException =>
    println("Resource not found")
  case e: HttpTimeoutException =>
    println("Request timed out, retrying...")
  case e: HttpServerErrorException =>
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
