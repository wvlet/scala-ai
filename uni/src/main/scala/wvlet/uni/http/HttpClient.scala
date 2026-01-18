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
  * Synchronous HTTP client interface.
  *
  * Retry is enabled by default. Use `withNoRetry` to disable.
  *
  * Example:
  * {{{
  * val client = HttpClient.create()
  *
  * // Simple request with default retry
  * val response = client.send(HttpRequest.get("/api/users"))
  *
  * // Disable retry for specific request
  * val response = client.withNoRetry.send(request)
  *
  * // Custom retry configuration
  * val response = client.withMaxRetry(5).send(request)
  * }}}
  */
trait HttpClient extends AutoCloseable:
  def config: HttpClientConfig

  /**
    * Send an HTTP request and return the response. Retries automatically based on config.
    */
  def send(request: HttpRequest): HttpResponse

  /**
    * Return a new client with retry disabled
    */
  def withNoRetry: HttpClient

  /**
    * Return a new client with specified max retry count
    */
  def withMaxRetry(maxRetries: Int): HttpClient

  /**
    * Return a new client with specified config
    */
  def withConfig(config: HttpClientConfig): HttpClient

  def close(): Unit = ()

/**
  * Asynchronous HTTP client interface using Rx.
  *
  * Retry is enabled by default. Use `withNoRetry` to disable.
  *
  * Example:
  * {{{
  * val client = AsyncHttpClient.create()
  *
  * // Simple async request
  * client.send(HttpRequest.get("/api/users"))
  *   .map(response => response.contentAsString)
  *   .run(println)
  *
  * // Disable retry
  * client.withNoRetry.send(request)
  * }}}
  */
trait AsyncHttpClient extends AutoCloseable:
  def config: HttpClientConfig

  /**
    * Send an HTTP request asynchronously, returning an Rx stream. Retries automatically based on config.
    */
  def send(request: HttpRequest): Rx[HttpResponse]

  /**
    * Return a new client with retry disabled
    */
  def withNoRetry: AsyncHttpClient

  /**
    * Return a new client with specified max retry count
    */
  def withMaxRetry(maxRetries: Int): AsyncHttpClient

  /**
    * Return a new client with specified config
    */
  def withConfig(config: HttpClientConfig): AsyncHttpClient

  def close(): Unit = ()

/**
  * HTTP client that supports streaming responses using Rx
  */
trait StreamingHttpClient extends AsyncHttpClient:
  /**
    * Send an HTTP request and stream the response as an Rx of byte chunks
    */
  def sendStreaming(request: HttpRequest): Rx[Array[Byte]]
