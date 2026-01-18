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

import wvlet.uni.control.Retry.RetryContext

/**
  * Exception thrown when the maximum number of retries is exceeded for an HTTP request. Provides
  * direct access to the HTTP response and status without Option types.
  */
class HttpMaxRetryException(
    val response: HttpResponse,
    val retryContext: RetryContext,
    cause: Throwable
) extends Exception(
      s"Reached max retry count ${retryContext.retryCount}/${retryContext.maxRetry}: ${cause
          .getMessage}",
      cause
    ):

  /**
    * The HTTP status from the response
    */
  def status: HttpStatus = response.status

  /**
    * The HTTP status code
    */
  def statusCode: Int = status.code

end HttpMaxRetryException

object HttpMaxRetryException:

  /**
    * Create an HttpMaxRetryException deriving the response from the cause. If the cause is an
    * HttpException with a response, use it. Otherwise, create a response from the status or fall
    * back to InternalServerError.
    */
  def fromCause(retryContext: RetryContext, cause: Throwable): HttpMaxRetryException =
    val response =
      cause match
        case e: HttpException =>
          e.response
            .getOrElse {
              e.status match
                case Some(s) =>
                  Response(s).withTextContent(e.getMessage)
                case None =>
                  Response.internalServerError(e.getMessage)
            }
        case e: HttpMaxRetryException =>
          e.response
        case _ =>
          Response.internalServerError(cause.getMessage)
    new HttpMaxRetryException(response, retryContext, cause)

end HttpMaxRetryException
