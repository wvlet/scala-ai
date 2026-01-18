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

import wvlet.uni.rx.Rx

/**
  * Synchronous HTTP client interface
  */
trait HttpClient extends AutoCloseable:
  def config: HttpClientConfig

  /**
    * Send an HTTP request and return the response
    */
  def send(request: HttpRequest): HttpResponse

  /**
    * Send a request with automatic retries based on configuration
    */
  def sendWithRetry(request: HttpRequest): HttpResponse =
    var lastException: Throwable = null
    var attempt                  = 0
    val maxAttempts              = config.retryConfig.maxRetries + 1

    while attempt < maxAttempts do
      try
        val response = send(request)
        if attempt < maxAttempts - 1 && config.retryConfig.isRetryable(response.status) then
          val delay = config.retryConfig.delayForAttempt(attempt)
          Thread.sleep(delay)
          attempt += 1
        else return response
      catch
        case e: HttpException if e.isRetryable && attempt < maxAttempts - 1 =>
          lastException = e
          val delay = config.retryConfig.delayForAttempt(attempt)
          Thread.sleep(delay)
          attempt += 1
        case e: Throwable =>
          throw e
    end while
    throw lastException

  // Convenience methods
  def get(uri: String): HttpResponse =
    send(HttpRequest.get(config.resolveUri(uri)))

  def get(uri: String, headers: HttpHeaders): HttpResponse =
    send(HttpRequest.get(config.resolveUri(uri)).withHeaders(headers))

  def post(uri: String, content: HttpContent): HttpResponse =
    send(HttpRequest.post(config.resolveUri(uri)).withContent(content))

  def post(uri: String, content: HttpContent, headers: HttpHeaders): HttpResponse =
    send(HttpRequest.post(config.resolveUri(uri)).withContent(content).withHeaders(headers))

  def postJson(uri: String, json: String): HttpResponse =
    send(
      HttpRequest
        .post(config.resolveUri(uri))
        .withJsonContent(json)
        .withContentType(ContentType.ApplicationJson)
    )

  def put(uri: String, content: HttpContent): HttpResponse =
    send(HttpRequest.put(config.resolveUri(uri)).withContent(content))

  def put(uri: String, content: HttpContent, headers: HttpHeaders): HttpResponse =
    send(HttpRequest.put(config.resolveUri(uri)).withContent(content).withHeaders(headers))

  def delete(uri: String): HttpResponse =
    send(HttpRequest.delete(config.resolveUri(uri)))

  def delete(uri: String, headers: HttpHeaders): HttpResponse =
    send(HttpRequest.delete(config.resolveUri(uri)).withHeaders(headers))

  def patch(uri: String, content: HttpContent): HttpResponse =
    send(HttpRequest.patch(config.resolveUri(uri)).withContent(content))

  def head(uri: String): HttpResponse =
    send(HttpRequest.head(config.resolveUri(uri)))

  def options(uri: String): HttpResponse =
    send(HttpRequest.options(config.resolveUri(uri)))

  def close(): Unit = ()

/**
  * Asynchronous HTTP client interface using Rx
  */
trait AsyncHttpClient extends AutoCloseable:
  def config: HttpClientConfig

  /**
    * Send an HTTP request asynchronously, returning an Rx stream
    */
  def sendAsync(request: HttpRequest): Rx[HttpResponse]

  // Convenience methods
  def getAsync(uri: String): Rx[HttpResponse] =
    sendAsync(HttpRequest.get(config.resolveUri(uri)))

  def getAsync(uri: String, headers: HttpHeaders): Rx[HttpResponse] =
    sendAsync(HttpRequest.get(config.resolveUri(uri)).withHeaders(headers))

  def postAsync(uri: String, content: HttpContent): Rx[HttpResponse] =
    sendAsync(HttpRequest.post(config.resolveUri(uri)).withContent(content))

  def postAsync(uri: String, content: HttpContent, headers: HttpHeaders): Rx[HttpResponse] =
    sendAsync(HttpRequest.post(config.resolveUri(uri)).withContent(content).withHeaders(headers))

  def postJsonAsync(uri: String, json: String): Rx[HttpResponse] =
    sendAsync(
      HttpRequest
        .post(config.resolveUri(uri))
        .withJsonContent(json)
        .withContentType(ContentType.ApplicationJson)
    )

  def putAsync(uri: String, content: HttpContent): Rx[HttpResponse] =
    sendAsync(HttpRequest.put(config.resolveUri(uri)).withContent(content))

  def deleteAsync(uri: String): Rx[HttpResponse] =
    sendAsync(HttpRequest.delete(config.resolveUri(uri)))

  def patchAsync(uri: String, content: HttpContent): Rx[HttpResponse] =
    sendAsync(HttpRequest.patch(config.resolveUri(uri)).withContent(content))

  def headAsync(uri: String): Rx[HttpResponse] =
    sendAsync(HttpRequest.head(config.resolveUri(uri)))

  def optionsAsync(uri: String): Rx[HttpResponse] =
    sendAsync(HttpRequest.options(config.resolveUri(uri)))

  def close(): Unit = ()

/**
  * Observer for streaming HTTP responses
  */
trait HttpResponseObserver:
  def onHeaders(status: HttpStatus, headers: HttpHeaders): Unit
  def onContent(chunk: Array[Byte]): Unit
  def onComplete(): Unit
  def onError(e: Throwable): Unit

/**
  * HTTP client that supports streaming responses using Rx
  */
trait StreamingHttpClient extends AsyncHttpClient:
  /**
    * Send an HTTP request and stream the response as an Rx of byte chunks
    */
  def sendStreaming(request: HttpRequest): Rx[Array[Byte]]

  /**
    * Send an HTTP request and stream the response through an observer
    */
  def sendStreaming(request: HttpRequest, observer: HttpResponseObserver): Unit
