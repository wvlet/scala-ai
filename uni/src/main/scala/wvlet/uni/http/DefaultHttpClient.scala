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

import java.util.concurrent.TimeUnit
import wvlet.uni.rx.Rx

/**
  * Default synchronous HTTP client implementation that wraps an HttpChannel with retry and redirect
  * handling.
  */
private[http] class DefaultHttpSyncClient(val config: HttpClientConfig, channel: HttpChannel)
    extends HttpSyncClient:

  override def send(request: HttpRequest): HttpResponse =
    val resolvedRequest = prepareRequest(request)
    sendWithRetry(resolvedRequest)

  private def prepareRequest(request: HttpRequest): HttpRequest =
    val uri = config.resolveUri(request.uri)
    var req = request.withUri(uri)

    // Apply default headers
    config
      .defaultHeaders
      .entries
      .foreach { header =>
        if !req.headers.contains(header.name) then
          req = req.addHeader(header.name, header.value)
      }

    // Apply user agent if set and not already present
    config
      .userAgent
      .foreach { ua =>
        if !req.headers.contains(HttpHeader.UserAgent) then
          req = req.addHeader(HttpHeader.UserAgent, ua)
      }

    req

  private def sendWithRetry(request: HttpRequest): HttpResponse = sendWithRetryLoop(
    request,
    attempt = 0,
    redirectCount = 0
  )

  private def sendWithRetryLoop(
      request: HttpRequest,
      attempt: Int,
      redirectCount: Int
  ): HttpResponse =
    val maxAttempts = config.retryConfig.maxRetries + 1
    try
      val response = channel.send(request, config)

      // Handle redirects - redirected requests also go through retry logic
      if config.followRedirects && response.isRedirection then
        handleRedirect(request, response, redirectCount)
      // Check if we should retry based on status
      else if attempt < maxAttempts - 1 && config.retryConfig.isRetryable(response.status) then
        val delay = config.retryConfig.delayForAttempt(attempt)
        Thread.sleep(delay)
        sendWithRetryLoop(request, attempt + 1, redirectCount)
      else
        response
    catch
      case e: HttpException if e.isRetryable && attempt < maxAttempts - 1 =>
        val delay = config.retryConfig.delayForAttempt(attempt)
        Thread.sleep(delay)
        sendWithRetryLoop(request, attempt + 1, redirectCount)

  private def handleRedirect(
      originalRequest: HttpRequest,
      response: HttpResponse,
      redirectCount: Int
  ): HttpResponse =
    if redirectCount >= config.maxRedirects then
      throw HttpException(
        s"Too many redirects (max: ${config.maxRedirects})",
        HttpErrorCode.TooManyRedirects
      )

    response.location match
      case Some(location) =>
        val redirectRequest = originalRequest
          .withUri(location)
          .withMethod(
            if response.status == HttpStatus.SeeOther_303 then
              HttpMethod.GET
            else
              originalRequest.method
          )
        // Redirected requests go through retry logic with reset attempt counter
        sendWithRetryLoop(redirectRequest, attempt = 0, redirectCount + 1)
      case None =>
        throw HttpException(
          "Redirect response missing Location header",
          HttpErrorCode.InvalidResponse
        )

  override def noRetry: HttpSyncClient = DefaultHttpSyncClient(config.noRetry, channel)

  override def withMaxRetry(maxRetries: Int): HttpSyncClient = DefaultHttpSyncClient(
    config.withRetryConfig(config.retryConfig.withMaxRetries(maxRetries)),
    channel
  )

  override def withConfig(newConfig: HttpClientConfig): HttpSyncClient = DefaultHttpSyncClient(
    newConfig,
    channel
  )

  override def close(): Unit = channel.close()

end DefaultHttpSyncClient

/**
  * Default asynchronous HTTP client implementation that wraps an HttpAsyncChannel with retry and
  * redirect handling.
  */
private[http] class DefaultHttpAsyncClient(val config: HttpClientConfig, channel: HttpAsyncChannel)
    extends HttpAsyncClient:

  override def send(request: HttpRequest): Rx[HttpResponse] =
    val resolvedRequest = prepareRequest(request)
    sendWithRetry(resolvedRequest, 0)

  override def sendStreaming(request: HttpRequest): Rx[Array[Byte]] =
    val resolvedRequest = prepareRequest(request)
    channel.sendStreaming(resolvedRequest, config)

  private def prepareRequest(request: HttpRequest): HttpRequest =
    val uri = config.resolveUri(request.uri)
    var req = request.withUri(uri)

    // Apply default headers
    config
      .defaultHeaders
      .entries
      .foreach { header =>
        if !req.headers.contains(header.name) then
          req = req.addHeader(header.name, header.value)
      }

    // Apply user agent if set and not already present
    config
      .userAgent
      .foreach { ua =>
        if !req.headers.contains(HttpHeader.UserAgent) then
          req = req.addHeader(HttpHeader.UserAgent, ua)
      }

    req

  private def sendWithRetry(request: HttpRequest, attempt: Int): Rx[HttpResponse] =
    sendWithRetryLoop(request, attempt, redirectCount = 0)

  private def sendWithRetryLoop(
      request: HttpRequest,
      attempt: Int,
      redirectCount: Int
  ): Rx[HttpResponse] = channel
    .send(request, config)
    .flatMap { response =>
      if config.followRedirects && response.isRedirection then
        handleRedirect(request, response, redirectCount)
      else if attempt < config.retryConfig.maxRetries &&
        config.retryConfig.isRetryable(response.status)
      then
        Rx.delay(config.retryConfig.delayForAttempt(attempt), TimeUnit.MILLISECONDS)
          .flatMap(_ => sendWithRetryLoop(request, attempt + 1, redirectCount))
      else
        Rx.single(response)
    }
    .recoverWith {
      case e: HttpException if e.isRetryable && attempt < config.retryConfig.maxRetries =>
        Rx.delay(config.retryConfig.delayForAttempt(attempt), TimeUnit.MILLISECONDS)
          .flatMap(_ => sendWithRetryLoop(request, attempt + 1, redirectCount))
    }

  private def handleRedirect(
      originalRequest: HttpRequest,
      response: HttpResponse,
      redirectCount: Int
  ): Rx[HttpResponse] =
    if redirectCount >= config.maxRedirects then
      Rx.exception(
        HttpException(
          s"Too many redirects (max: ${config.maxRedirects})",
          HttpErrorCode.TooManyRedirects
        )
      )
    else
      response.location match
        case Some(location) =>
          val redirectRequest = originalRequest
            .withUri(location)
            .withMethod(
              if response.status == HttpStatus.SeeOther_303 then
                HttpMethod.GET
              else
                originalRequest.method
            )
          // Redirected requests go through retry logic with reset attempt counter
          sendWithRetryLoop(redirectRequest, attempt = 0, redirectCount + 1)
        case None =>
          Rx.exception(
            HttpException(
              "Redirect response missing Location header",
              HttpErrorCode.InvalidResponse
            )
          )

  override def noRetry: HttpAsyncClient = DefaultHttpAsyncClient(config.noRetry, channel)

  override def withMaxRetry(maxRetries: Int): HttpAsyncClient = DefaultHttpAsyncClient(
    config.withRetryConfig(config.retryConfig.withMaxRetries(maxRetries)),
    channel
  )

  override def withConfig(newConfig: HttpClientConfig): HttpAsyncClient = DefaultHttpAsyncClient(
    newConfig,
    channel
  )

  override def close(): Unit = channel.close()

end DefaultHttpAsyncClient

private[http] object DefaultHttpSyncClient:
  def apply(config: HttpClientConfig, channel: HttpChannel): DefaultHttpSyncClient =
    new DefaultHttpSyncClient(config, channel)

private[http] object DefaultHttpAsyncClient:
  def apply(config: HttpClientConfig, channel: HttpAsyncChannel): DefaultHttpAsyncClient =
    new DefaultHttpAsyncClient(config, channel)
