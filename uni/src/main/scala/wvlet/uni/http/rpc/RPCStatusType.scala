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
package wvlet.uni.http.rpc

import wvlet.uni.http.HttpStatus

/**
  * RPC status type classification.
  *
  * Status types categorize RPC errors into groups that indicate whether the client should retry:
  *   - SUCCESS: Request completed successfully
  *   - USER_ERROR: Client error, not retryable (maps to 4xx HTTP)
  *   - INTERNAL_ERROR: Server error, generally retryable (maps to 5xx HTTP)
  *   - RESOURCE_EXHAUSTED: Resource limits exceeded, retry after backoff (maps to 429 HTTP)
  */
sealed trait RPCStatusType:
  /**
    * Single-character prefix used in status code names (S, U, I, R)
    */
  def prefix: String

  /**
    * The error code range [minCode, maxCode)
    */
  def codeRange: (Int, Int)

  def minCode: Int = codeRange._1
  def maxCode: Int = codeRange._2

  def isValidCode(code: Int): Boolean = minCode <= code && code < maxCode

  def isValidHttpStatus(httpStatus: HttpStatus): Boolean

  def name: String = toString

end RPCStatusType

object RPCStatusType:

  def ofPrefix(prefix: Char): RPCStatusType =
    val p = prefix.toString
    all
      .find(_.prefix == p)
      .getOrElse(throw IllegalArgumentException(s"Unknown RPCStatus code prefix: ${prefix}"))

  /**
    * Successful responses
    */
  case object SUCCESS extends RPCStatusType:
    override def prefix: String                                     = "S"
    override def codeRange: (Int, Int)                              = (0, 1000)
    override def isValidHttpStatus(httpStatus: HttpStatus): Boolean = httpStatus.isSuccessful

  /**
    * User/client errors - not retryable in general
    */
  case object USER_ERROR extends RPCStatusType:
    override def prefix: String                                     = "U"
    override def codeRange: (Int, Int)                              = (1000, 2000)
    override def isValidHttpStatus(httpStatus: HttpStatus): Boolean = httpStatus.isClientError

  /**
    * Server internal errors - generally retryable
    */
  case object INTERNAL_ERROR extends RPCStatusType:
    override def prefix: String                                     = "I"
    override def codeRange: (Int, Int)                              = (2000, 3000)
    override def isValidHttpStatus(httpStatus: HttpStatus): Boolean = httpStatus.isServerError

  /**
    * Resource exhausted or quota exceeded - retry after backoff
    */
  case object RESOURCE_EXHAUSTED extends RPCStatusType:
    override def prefix: String                                     = "R"
    override def codeRange: (Int, Int)                              = (3000, 4000)
    override def isValidHttpStatus(httpStatus: HttpStatus): Boolean =
      httpStatus == HttpStatus.TooManyRequests_429

  def all: Seq[RPCStatusType] = Seq(SUCCESS, USER_ERROR, INTERNAL_ERROR, RESOURCE_EXHAUSTED)

  def unapply(s: String): Option[RPCStatusType] =
    val name = s.toUpperCase
    all.find(_.toString == name)

end RPCStatusType
