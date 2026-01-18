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
  * Entry point for HTTP client and server creation.
  *
  * Example:
  * {{{
  * // Create a sync client
  * val client = Http.client.newSyncClient
  * val response = client.send(HttpRequest.get("/api/users"))
  *
  * // Create an async client with custom config
  * val asyncClient = Http.client
  *   .withBaseUri("https://api.example.com")
  *   .withMaxRetry(5)
  *   .newAsyncClient
  *
  * // Future: Create a server
  * // val server = Http.server.withPort(8080).start()
  * }}}
  */
object Http:
  /**
    * Entry point for creating HTTP clients. Uses the default platform-specific channel factory.
    */
  def client: HttpClientBuilder = HttpClientBuilder(HttpClientConfig.default, defaultChannelFactory)

  /**
    * Create a client builder with a custom channel factory
    */
  def client(channelFactory: HttpChannelFactory): HttpClientBuilder =
    HttpClientBuilder(HttpClientConfig.default, channelFactory)

  /**
    * Platform-specific default channel factory. This will be provided by platform-specific modules (.jvm, .js, .native).
    */
  private[http] var defaultChannelFactory: HttpChannelFactory = NoOpChannelFactory

  /**
    * Set the default channel factory. Called by platform-specific initialization.
    */
  def setDefaultChannelFactory(factory: HttpChannelFactory): Unit =
    defaultChannelFactory = factory

/**
  * Builder for creating HTTP clients with configuration
  */
case class HttpClientBuilder(config: HttpClientConfig, channelFactory: HttpChannelFactory):
  def withConfig(config: HttpClientConfig): HttpClientBuilder =
    copy(config = config)

  def withChannelFactory(factory: HttpChannelFactory): HttpClientBuilder =
    copy(channelFactory = factory)

  def withBaseUri(uri: String): HttpClientBuilder =
    copy(config = config.withBaseUri(uri))

  def noBaseUri: HttpClientBuilder =
    copy(config = config.noBaseUri)

  def withConnectTimeoutMillis(millis: Long): HttpClientBuilder =
    copy(config = config.withConnectTimeoutMillis(millis))

  def withReadTimeoutMillis(millis: Long): HttpClientBuilder =
    copy(config = config.withReadTimeoutMillis(millis))

  def withMaxRetry(maxRetries: Int): HttpClientBuilder =
    copy(config = config.withRetryConfig(config.retryConfig.withMaxRetries(maxRetries)))

  def noRetry: HttpClientBuilder =
    copy(config = config.noRetry)

  def withUserAgent(userAgent: String): HttpClientBuilder =
    copy(config = config.withUserAgent(userAgent))

  def noUserAgent: HttpClientBuilder =
    copy(config = config.noUserAgent)

  def withDefaultHeader(name: String, value: String): HttpClientBuilder =
    copy(config = config.addDefaultHeader(name, value))

  def noFollowRedirects: HttpClientBuilder =
    copy(config = config.noFollowRedirects)

  def withMaxRedirects(max: Int): HttpClientBuilder =
    copy(config = config.withMaxRedirects(max))

  /**
    * Create a new synchronous HTTP client with the configured settings.
    */
  def newSyncClient: HttpSyncClient =
    DefaultHttpSyncClient(config, channelFactory.newChannel)

  /**
    * Create a new asynchronous HTTP client with the configured settings.
    */
  def newAsyncClient: HttpAsyncClient =
    DefaultHttpAsyncClient(config, channelFactory.newAsyncChannel)

/**
  * No-op channel factory used when no platform-specific implementation is available
  */
private object NoOpChannelFactory extends HttpChannelFactory:
  def newChannel: HttpChannel =
    throw NotImplementedError("No HttpChannel implementation available. Import a platform-specific module.")

  def newAsyncChannel: HttpAsyncChannel =
    throw NotImplementedError("No HttpAsyncChannel implementation available. Import a platform-specific module.")
