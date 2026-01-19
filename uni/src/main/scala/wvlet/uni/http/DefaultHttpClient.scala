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

import wvlet.uni.control.ResultClass
import wvlet.uni.control.Retry
import wvlet.uni.rx.Rx

/**
  * Default synchronous HTTP client implementation that wraps an HttpChannel with retry and redirect
  * handling.
  */
private[http] class DefaultHttpSyncClient(val config: HttpClientConfig, channel: HttpChannel)
    extends HttpSyncClient:

  override def send(request: HttpRequest): HttpResponse =
    val resolvedRequest = prepareRequest(request)
    sendWithRedirectHandling(resolvedRequest, redirectCount = 0)

  private def prepareRequest(request: HttpRequest): HttpRequest =
    val uri = config.resolveUri(request.uri)
    config.requestFilter(request.withUri(uri))

  private def sendWithRedirectHandling(request: HttpRequest, redirectCount: Int): HttpResponse =
    val response = sendWithRetry(request)

    if config.followRedirects && response.isRedirection then
      handleRedirect(request, response, redirectCount)
    else
      response

  private def sendWithRetry(request: HttpRequest): HttpResponse =
    val retryContext = config
      .retryContext
      .withResultClassifier[HttpResponse](HttpExceptionClassifier.classifyHttpResponse)
      .withErrorClassifier(HttpExceptionClassifier.classifyExecutionFailure)

    try
      retryContext.run {
        channel.send(request, config)
      }
    catch
      case e: Retry.MaxRetryException =>
        throw HttpMaxRetryException.fromCause(e.retryContext, e.retryContext.lastError)

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
        // Per RFC 7231: 301/302/303 redirects should change POST to GET
        val shouldChangeToGet =
          originalRequest.method == HttpMethod.POST && (
            response.status == HttpStatus.MovedPermanently_301 ||
              response.status == HttpStatus.Found_302 || response.status == HttpStatus.SeeOther_303
          )
        val redirectRequest = originalRequest
          .withUri(location)
          .withMethod(
            if shouldChangeToGet then
              HttpMethod.GET
            else
              originalRequest.method
          )
        sendWithRedirectHandling(redirectRequest, redirectCount + 1)
      case None =>
        throw HttpException(
          "Redirect response missing Location header",
          HttpErrorCode.InvalidResponse
        )

  end handleRedirect

  override def noRetry: HttpSyncClient = DefaultHttpSyncClient(config.noRetry, channel)

  override def withMaxRetry(maxRetries: Int): HttpSyncClient = DefaultHttpSyncClient(
    config.withMaxRetry(maxRetries),
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
    sendWithRedirectHandling(resolvedRequest, redirectCount = 0)

  override def sendStreaming(request: HttpRequest): Rx[Array[Byte]] =
    val resolvedRequest = prepareRequest(request)
    channel.sendStreaming(resolvedRequest, config)

  override def sendSSE(request: HttpRequest): Rx[ServerSentEvent] =
    // Ensure Accept header is set for SSE
    val sseRequest =
      if request.hasHeader(HttpHeader.Accept) then
        request
      else
        request.withAcceptEventStream

    val resolvedRequest = prepareRequest(sseRequest)
    val parser          = ServerSentEventParser()
    val handler         = request.eventHandler

    // Note: onConnect callback is not invoked because sendStreaming doesn't provide
    // access to the HttpResponse headers. This is a limitation of the current API.
    // Similarly, charset detection from Content-Type header is not supported;
    // UTF-8 is used as the default per SSE specification.

    // Use sendStreaming and parse the chunks.
    // After the stream completes, flush the parser to emit any remaining buffered data.
    val streamEvents = sendStreaming(resolvedRequest).flatMap { chunk =>
      val text   = String(chunk, "UTF-8")
      val events = parser.feed(text)
      Rx.fromSeq(events)
    }

    // Flush any remaining data after stream completes
    val flushEvent = Rx
      .single(parser.flush())
      .flatMap {
        case Some(event) =>
          Rx.single(event)
        case None =>
          Rx.empty
      }

    streamEvents
      .concat(flushEvent)
      .tap { event =>
        handler.onEvent(event)
      }
      .tapOnFailure(handler.onError)

  end sendSSE

  private def prepareRequest(request: HttpRequest): HttpRequest =
    val uri = config.resolveUri(request.uri)
    config.requestFilter(request.withUri(uri))

  private def sendWithRedirectHandling(request: HttpRequest, redirectCount: Int): Rx[HttpResponse] =
    sendWithRetry(request).flatMap { response =>
      if config.followRedirects && response.isRedirection then
        handleRedirect(request, response, redirectCount)
      else
        Rx.single(response)
    }

  private def sendWithRetry(request: HttpRequest): Rx[HttpResponse] =
    val retryContext = config
      .retryContext
      .withResultClassifier[HttpResponse](HttpExceptionClassifier.classifyHttpResponse)
      .withErrorClassifier(HttpExceptionClassifier.classifyExecutionFailure)

    retryContext
      .runAsyncWithContext(request) {
        channel.send(request, config)
      }
      .recover { case e: Retry.MaxRetryException =>
        throw HttpMaxRetryException.fromCause(e.retryContext, e.retryContext.lastError)
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
          // Per RFC 7231: 301/302/303 redirects should change POST to GET
          val shouldChangeToGet =
            originalRequest.method == HttpMethod.POST && (
              response.status == HttpStatus.MovedPermanently_301 ||
                response.status == HttpStatus.Found_302 ||
                response.status == HttpStatus.SeeOther_303
            )
          val redirectRequest = originalRequest
            .withUri(location)
            .withMethod(
              if shouldChangeToGet then
                HttpMethod.GET
              else
                originalRequest.method
            )
          sendWithRedirectHandling(redirectRequest, redirectCount + 1)
        case None =>
          Rx.exception(
            HttpException(
              "Redirect response missing Location header",
              HttpErrorCode.InvalidResponse
            )
          )

  override def noRetry: HttpAsyncClient = DefaultHttpAsyncClient(config.noRetry, channel)

  override def withMaxRetry(maxRetries: Int): HttpAsyncClient = DefaultHttpAsyncClient(
    config.withMaxRetry(maxRetries),
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
