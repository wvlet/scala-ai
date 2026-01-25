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
enum RPCStatusType(val prefix: String, val minCode: Int, val maxCode: Int):

  /** Successful responses */
  case SUCCESS extends RPCStatusType("S", 0, 1000)

  /** User/client errors - not retryable in general */
  case USER_ERROR extends RPCStatusType("U", 1000, 2000)

  /** Server internal errors - generally retryable */
  case INTERNAL_ERROR extends RPCStatusType("I", 2000, 3000)

  /** Resource exhausted or quota exceeded - retry after backoff */
  case RESOURCE_EXHAUSTED extends RPCStatusType("R", 3000, 4000)

  def codeRange: (Int, Int) = (minCode, maxCode)

  def isValidCode(code: Int): Boolean = minCode <= code && code < maxCode

  def isValidHttpStatus(httpStatus: HttpStatus): Boolean =
    this match
      case SUCCESS =>
        httpStatus.isSuccessful
      case USER_ERROR =>
        httpStatus.isClientError
      case INTERNAL_ERROR =>
        httpStatus.isServerError
      case RESOURCE_EXHAUSTED =>
        httpStatus == HttpStatus.TooManyRequests_429

end RPCStatusType

object RPCStatusType:

  def ofPrefix(prefix: Char): RPCStatusType =
    val p = prefix.toString
    RPCStatusType
      .values
      .find(_.prefix == p)
      .getOrElse(throw IllegalArgumentException(s"Unknown RPCStatusType prefix: ${prefix}"))

  def unapply(s: String): Option[RPCStatusType] =
    val name = s.toUpperCase
    RPCStatusType.values.find(_.toString == name)

end RPCStatusType
