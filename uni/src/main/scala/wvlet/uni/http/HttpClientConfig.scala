/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.uni.http

/**
  * Configuration for HTTP clients. Includes channel factory for creating platform-specific clients.
  */
case class HttpClientConfig(
    baseUri: Option[String] = None,
    connectTimeoutMillis: Long = 30000,
    readTimeoutMillis: Long = 60000,
    followRedirects: Boolean = true,
    maxRedirects: Int = 10,
    retryConfig: HttpRetryConfig = HttpRetryConfig.default,
    defaultHeaders: HttpHeaders = HttpHeaders.empty,
    userAgent: Option[String] = None,
    channelFactory: HttpChannelFactory = HttpClientConfig.NoOpChannelFactory
):
  def withBaseUri(uri: String): HttpClientConfig = copy(baseUri = Some(uri))
  def noBaseUri: HttpClientConfig                = copy(baseUri = None)

  def withConnectTimeoutMillis(millis: Long): HttpClientConfig = copy(connectTimeoutMillis = millis)
  def withReadTimeoutMillis(millis: Long): HttpClientConfig    = copy(readTimeoutMillis = millis)

  def withFollowRedirects(follow: Boolean): HttpClientConfig = copy(followRedirects = follow)
  def noFollowRedirects: HttpClientConfig                    = copy(followRedirects = false)

  def withMaxRedirects(max: Int): HttpClientConfig = copy(maxRedirects = max)

  def withRetryConfig(config: HttpRetryConfig): HttpClientConfig = copy(retryConfig = config)
  def noRetry: HttpClientConfig = copy(retryConfig = HttpRetryConfig.noRetry)

  def withDefaultHeaders(h: HttpHeaders): HttpClientConfig = copy(defaultHeaders = h)

  def addDefaultHeader(name: String, value: String): HttpClientConfig = copy(defaultHeaders =
    defaultHeaders.add(name, value)
  )

  def setDefaultHeader(name: String, value: String): HttpClientConfig = copy(defaultHeaders =
    defaultHeaders.set(name, value)
  )

  def withUserAgent(ua: String): HttpClientConfig = copy(userAgent = Some(ua))
  def noUserAgent: HttpClientConfig               = copy(userAgent = None)

  def withContentType(ct: ContentType): HttpClientConfig = setDefaultHeader(
    HttpHeader.ContentType,
    ct.value
  )

  def withAccept(accept: String): HttpClientConfig = setDefaultHeader(HttpHeader.Accept, accept)

  def withAcceptJson: HttpClientConfig = withAccept(ContentType.ApplicationJson.value)

  def withChannelFactory(factory: HttpChannelFactory): HttpClientConfig = copy(channelFactory =
    factory
  )

  def withMaxRetry(maxRetries: Int): HttpClientConfig = copy(retryConfig =
    retryConfig.withMaxRetries(maxRetries)
  )

  def resolveUri(uri: String): String =
    baseUri match
      case Some(base) if !uri.startsWith("http://") && !uri.startsWith("https://") =>
        if base.endsWith("/") && uri.startsWith("/") then
          s"${base.dropRight(1)}${uri}"
        else if !base.endsWith("/") && !uri.startsWith("/") then
          s"${base}/${uri}"
        else
          s"${base}${uri}"
      case _ =>
        uri

  /**
    * Create a new synchronous HTTP client with this configuration.
    */
  def newSyncClient: HttpSyncClient = DefaultHttpSyncClient(this, channelFactory.newChannel)

  /**
    * Create a new asynchronous HTTP client with this configuration.
    */
  def newAsyncClient: HttpAsyncClient = DefaultHttpAsyncClient(this, channelFactory.newAsyncChannel)

end HttpClientConfig

object HttpClientConfig:
  val default: HttpClientConfig = HttpClientConfig()

  /**
    * No-op channel factory used when no platform-specific implementation is available
    */
  private[http] object NoOpChannelFactory extends HttpChannelFactory:
    def newChannel: HttpChannel =
      throw NotImplementedError(
        "No HttpChannel implementation available. Import a platform-specific module."
      )

    def newAsyncChannel: HttpAsyncChannel =
      throw NotImplementedError(
        "No HttpAsyncChannel implementation available. Import a platform-specific module."
      )

/**
  * Configuration for HTTP request retry behavior
  */
case class HttpRetryConfig(
    maxRetries: Int = 3,
    initialDelayMillis: Long = 1000,
    maxDelayMillis: Long = 30000,
    backoffMultiplier: Double = 2.0,
    retryableStatuses: Set[Int] = Set(408, 429, 500, 502, 503, 504)
):
  def withMaxRetries(max: Int): HttpRetryConfig                  = copy(maxRetries = max)
  def withInitialDelayMillis(millis: Long): HttpRetryConfig      = copy(initialDelayMillis = millis)
  def withMaxDelayMillis(millis: Long): HttpRetryConfig          = copy(maxDelayMillis = millis)
  def withBackoffMultiplier(mult: Double): HttpRetryConfig       = copy(backoffMultiplier = mult)
  def withRetryableStatuses(statuses: Set[Int]): HttpRetryConfig = copy(retryableStatuses =
    statuses
  )

  def isRetryable(status: HttpStatus): Boolean = retryableStatuses.contains(status.code)

  def delayForAttempt(attempt: Int): Long =
    val delay = initialDelayMillis * Math.pow(backoffMultiplier, attempt.toDouble)
    Math.min(delay.toLong, maxDelayMillis)

object HttpRetryConfig:
  val default: HttpRetryConfig = HttpRetryConfig()
  val noRetry: HttpRetryConfig = HttpRetryConfig(maxRetries = 0)
