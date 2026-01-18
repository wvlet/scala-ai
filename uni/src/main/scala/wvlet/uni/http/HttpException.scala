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
  * Error codes for HTTP-related errors
  */
enum HttpErrorCode:
  // Connection errors
  case ConnectionFailed
  case ConnectionTimeout
  case ReadTimeout
  case SslError

  // Request errors
  case InvalidRequest
  case RequestCancelled

  // Response errors
  case ClientError  // 4xx
  case ServerError  // 5xx
  case UnexpectedResponse

  // Other errors
  case Unknown

  def isRetryable: Boolean = this match
    case ConnectionTimeout | ReadTimeout | ServerError => true
    case _                                             => false

/**
  * Base exception for all HTTP-related errors.
  *
  * Uses HttpErrorCode to categorize errors instead of many subclasses.
  */
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

  def retryAfter: Option[Long] =
    response.flatMap(_.headers.get(HttpHeader.RetryAfter)).flatMap(_.toLongOption)

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

  def apply(message: String, cause: Throwable): HttpException =
    HttpException(message, HttpErrorCode.Unknown, None, None, None, cause)

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

  def sslError(message: String, cause: Throwable = null): HttpException =
    HttpException(message, HttpErrorCode.SslError, None, None, None, cause)

  def clientError(message: String, status: HttpStatus, response: Option[HttpResponse] = None): HttpException =
    HttpException(message, HttpErrorCode.ClientError, Some(status), None, response, null)

  def serverError(message: String, status: HttpStatus, response: Option[HttpResponse] = None): HttpException =
    HttpException(message, HttpErrorCode.ServerError, Some(status), None, response, null)
