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

import wvlet.uni.control.CircuitBreakerOpenException
import wvlet.uni.control.ResultClass
import wvlet.uni.control.ResultClass.{Failed, Succeeded, nonRetryableFailure, retryableFailure}

import java.io.EOFException
import java.util.concurrent.TimeoutException

/**
  * Classification logic for HTTP responses and exceptions to determine retry behavior.
  *
  * This provides comprehensive retry classification ported from airframe-http, including:
  *   - Nuanced HTTP status code classification (5xx retryable, selective 4xx)
  *   - Platform-specific exception handling via HttpCompat
  *   - Root cause exception unwrapping
  */
object HttpExceptionClassifier:

  /**
    * Classify an HTTP response to determine if it succeeded or failed (and if retryable).
    *
    * Classification rules:
    *   - 2xx: Success
    *   - 304: Success (cached content valid)
    *   - 5xx: Retryable (503 with extra wait for rate limiting)
    *   - 4xx: Selective - 408, 410, 429, 499 are retryable; others are not
    */
  def classifyHttpResponse(response: HttpResponse): ResultClass =
    val status = response.status
    status match
      case s if s.isSuccessful =>
        Succeeded
      case HttpStatus.NotModified_304 =>
        // 304 is a successful response indicating cached content is still valid
        Succeeded
      case s if s.isServerError =>
        val f = retryableFailure(HttpException.fromResponse(response))
        if status == HttpStatus.ServiceUnavailable_503 then
          // Server is busy; reduce request rate with extra wait
          f.withExtraWaitFactor(0.5)
        else
          f
      case s if s.isClientError =>
        classifyClientError(response, status)
      case _ =>
        nonRetryableFailure(HttpException.fromResponse(response))

  private def classifyClientError(response: HttpResponse, status: HttpStatus): Failed =
    status match
      case HttpStatus.BadRequest_400 if isRetryable400ErrorMessage(response.contentAsString) =>
        retryableFailure(HttpException.fromResponse(response))
      case HttpStatus.RequestTimeout_408 =>
        retryableFailure(HttpException.fromResponse(response))
      case HttpStatus.Gone_410 =>
        // Server may have failed to process the request
        retryableFailure(HttpException.fromResponse(response))
      case HttpStatus.TooManyRequests_429 =>
        // Rate limiting - should retry with backoff
        retryableFailure(HttpException.fromResponse(response))
      case HttpStatus.ClientClosedRequest_499 =>
        // Client may have closed connection prematurely
        retryableFailure(HttpException.fromResponse(response))
      case _ =>
        nonRetryableFailure(HttpException.fromResponse(response))

  private def isRetryable400ErrorMessage(content: Option[String]): Boolean = content.exists { m =>
    // OkHttp may close connections with this message
    m.contains("Idle connections will be closed")
  }

  /**
    * Classify an execution failure (thrown exception) to determine retry behavior. This composes
    * common exception classifiers with platform-specific classifiers from HttpCompat.
    */
  def classifyExecutionFailure(ex: Throwable): Failed = executionFailureClassifier.applyOrElse(
    ex,
    nonRetryable
  )

  /**
    * Common exception classifier usable on all platforms (no JVM-specific imports).
    */
  def commonExceptionClassifier: PartialFunction[Throwable, Failed] =
    case e: CircuitBreakerOpenException =>
      nonRetryableFailure(e)
    case e: EOFException =>
      retryableFailure(e)
    case e: TimeoutException =>
      retryableFailure(e)
    case e: HttpException if e.isRetryable =>
      retryableFailure(e)
    case e: HttpException if !e.isRetryable =>
      nonRetryableFailure(e)

  /**
    * Full exception classifier that composes platform-specific classifiers.
    */
  def executionFailureClassifier: PartialFunction[Throwable, Failed] =
    commonExceptionClassifier orElse HttpCompat.sslExceptionClassifier orElse
      HttpCompat.connectionExceptionClassifier orElse HttpCompat.rootCauseExceptionClassifier

  /**
    * Root cause classifier for ScalaJS/Native (no java.lang.reflect). Used by platform-specific
    * HttpCompat implementations.
    */
  def rootCauseExceptionClassifierSimple: PartialFunction[Throwable, Failed] =
    case e if e.getCause != null =>
      classifyExecutionFailure(e.getCause)

  private def nonRetryable: Throwable => Failed = e => nonRetryableFailure(e)

end HttpExceptionClassifier
