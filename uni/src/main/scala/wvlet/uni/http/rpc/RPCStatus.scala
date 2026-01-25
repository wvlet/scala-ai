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
  * Standard RPC status codes for generic RPC service implementation.
  *
  * Status naming convention: {DESCRIPTION}_{TYPE}{NUMBER}
  *   - TYPE: S (success), U (user error), I (internal error), R (resource exhausted)
  *   - NUMBER: Sequential number within the type category
  *
  * Examples: SUCCESS_S0, INVALID_REQUEST_U1, INTERNAL_ERROR_I0, RESOURCE_EXHAUSTED_R0
  */
object RPCStatus:

  import RPCStatusType.*

  // Lazy to avoid initialization order issues
  private lazy val codeTable: Map[Int, RPCStatus]        = all.map(x => x.code -> x).toMap
  private lazy val codeNameTable: Map[String, RPCStatus] = all.map(x => x.name -> x).toMap

  // HTTP status to RPC status mapping
  private lazy val httpStatusMapping: Map[HttpStatus, RPCStatus] = Map(
    // 2xx Success
    HttpStatus.Ok_200        -> SUCCESS_S0,
    HttpStatus.Created_201   -> SUCCESS_S0,
    HttpStatus.Accepted_202  -> SUCCESS_S0,
    HttpStatus.NoContent_204 -> SUCCESS_S0,
    // 4xx Client errors
    HttpStatus.BadRequest_400          -> INVALID_REQUEST_U1,
    HttpStatus.Unauthorized_401        -> UNAUTHENTICATED_U13,
    HttpStatus.Forbidden_403           -> PERMISSION_DENIED_U14,
    HttpStatus.NotFound_404            -> NOT_FOUND_U5,
    HttpStatus.MethodNotAllowed_405    -> NOT_SUPPORTED_U7,
    HttpStatus.Conflict_409            -> ABORTED_U12,
    HttpStatus.PreconditionFailed_412  -> UNEXPECTED_STATE_U9,
    HttpStatus.UnprocessableEntity_422 -> INVALID_REQUEST_U1,
    HttpStatus.TooManyRequests_429     -> RESOURCE_EXHAUSTED_R0,
    // 5xx Server errors
    HttpStatus.InternalServerError_500 -> INTERNAL_ERROR_I0,
    HttpStatus.NotImplemented_501      -> UNIMPLEMENTED_U8,
    HttpStatus.BadGateway_502          -> UNAVAILABLE_I2,
    HttpStatus.ServiceUnavailable_503  -> UNAVAILABLE_I2,
    HttpStatus.GatewayTimeout_504      -> DEADLINE_EXCEEDED_I4
  )

  def ofCode(code: Int): RPCStatus = codeTable.getOrElse(
    code,
    throw IllegalArgumentException(s"Invalid RPCStatus code: ${code}")
  )

  def ofCodeName(name: String): RPCStatus = codeNameTable.getOrElse(
    name,
    throw IllegalArgumentException(s"Invalid RPCStatus name: ${name}")
  )

  /**
    * Map HttpStatus to RPCStatus
    */
  def fromHttpStatus(httpStatus: HttpStatus): RPCStatus =
    httpStatusMapping.get(httpStatus) match
      case Some(status) =>
        status
      case None =>
        if httpStatus.isSuccessful then
          SUCCESS_S0
        else if httpStatus.isClientError then
          USER_ERROR_U0
        else if httpStatus.isServerError then
          INTERNAL_ERROR_I0
        else
          UNKNOWN_I1

  def all: Seq[RPCStatus] = successes ++ userErrors ++ internalErrors ++ resourceErrors

  private def successes: Seq[RPCStatus] = Seq(SUCCESS_S0)

  private def userErrors: Seq[RPCStatus] = Seq(
    USER_ERROR_U0,
    INVALID_REQUEST_U1,
    INVALID_ARGUMENT_U2,
    SYNTAX_ERROR_U3,
    OUT_OF_RANGE_U4,
    NOT_FOUND_U5,
    ALREADY_EXISTS_U6,
    NOT_SUPPORTED_U7,
    UNIMPLEMENTED_U8,
    UNEXPECTED_STATE_U9,
    INCONSISTENT_STATE_U10,
    CANCELLED_U11,
    ABORTED_U12,
    UNAUTHENTICATED_U13,
    PERMISSION_DENIED_U14
  )

  private def internalErrors: Seq[RPCStatus] = Seq(
    INTERNAL_ERROR_I0,
    UNKNOWN_I1,
    UNAVAILABLE_I2,
    TIMEOUT_I3,
    DEADLINE_EXCEEDED_I4,
    INTERRUPTED_I5,
    SERVICE_STARTING_UP_I6,
    SERVICE_SHUTTING_DOWN_I7,
    DATA_LOSS_I8
  )

  private def resourceErrors: Seq[RPCStatus] = Seq(
    RESOURCE_EXHAUSTED_R0,
    OUT_OF_MEMORY_R1,
    EXCEEDED_RATE_LIMIT_R2,
    EXCEEDED_CPU_LIMIT_R3,
    EXCEEDED_MEMORY_LIMIT_R4,
    EXCEEDED_TIME_LIMIT_R5,
    EXCEEDED_DATA_SIZE_LIMIT_R6,
    EXCEEDED_STORAGE_LIMIT_R7,
    EXCEEDED_BUDGET_R8
  )

  // ============ Success ============
  case object SUCCESS_S0 extends RPCStatus(SUCCESS, HttpStatus.Ok_200)

  // ============ User Errors (non-retryable, 4xx) ============

  /** Generic user error */
  case object USER_ERROR_U0 extends RPCStatus(USER_ERROR, HttpStatus.BadRequest_400)

  /** Invalid RPC request - user should not retry */
  case object INVALID_REQUEST_U1 extends RPCStatus(USER_ERROR, HttpStatus.BadRequest_400)

  /** RPC request arguments have invalid values */
  case object INVALID_ARGUMENT_U2 extends RPCStatus(USER_ERROR, HttpStatus.BadRequest_400)

  /** Syntax error in an RPC argument */
  case object SYNTAX_ERROR_U3 extends RPCStatus(USER_ERROR, HttpStatus.BadRequest_400)

  /** Invalid range data in request argument */
  case object OUT_OF_RANGE_U4 extends RPCStatus(USER_ERROR, HttpStatus.BadRequest_400)

  /** Requested resource or RPC method not found */
  case object NOT_FOUND_U5 extends RPCStatus(USER_ERROR, HttpStatus.NotFound_404)

  /** Resource creation failed - already exists */
  case object ALREADY_EXISTS_U6 extends RPCStatus(USER_ERROR, HttpStatus.Conflict_409)

  /** Requested RPC method not supported */
  case object NOT_SUPPORTED_U7 extends RPCStatus(USER_ERROR, HttpStatus.MethodNotAllowed_405)

  /** Requested RPC method not implemented */
  case object UNIMPLEMENTED_U8 extends RPCStatus(USER_ERROR, HttpStatus.NotImplemented_501)

  /** Precondition not met - client should not retry until state is fixed */
  case object UNEXPECTED_STATE_U9 extends RPCStatus(USER_ERROR, HttpStatus.PreconditionFailed_412)

  /** Inconsistent state - client should not retry until state is fixed */
  case object INCONSISTENT_STATE_U10
      extends RPCStatus(USER_ERROR, HttpStatus.PreconditionFailed_412)

  /** Request cancelled by client */
  case object CANCELLED_U11 extends RPCStatus(USER_ERROR, HttpStatus.BadRequest_400)

  /** Request aborted (deadlock, transaction conflict) - retry at higher level */
  case object ABORTED_U12 extends RPCStatus(USER_ERROR, HttpStatus.Conflict_409)

  /** User not authenticated */
  case object UNAUTHENTICATED_U13 extends RPCStatus(USER_ERROR, HttpStatus.Unauthorized_401)

  /** User lacks permission to access resource */
  case object PERMISSION_DENIED_U14 extends RPCStatus(USER_ERROR, HttpStatus.Forbidden_403)

  // ============ Internal Errors (retryable, 5xx) ============

  /** Internal failure - user can retry */
  case object INTERNAL_ERROR_I0
      extends RPCStatus(INTERNAL_ERROR, HttpStatus.InternalServerError_500)

  /** Unknown internal error */
  case object UNKNOWN_I1 extends RPCStatus(INTERNAL_ERROR, HttpStatus.InternalServerError_500)

  /** Service unavailable */
  case object UNAVAILABLE_I2 extends RPCStatus(INTERNAL_ERROR, HttpStatus.ServiceUnavailable_503)

  /** Service did not respond in time */
  case object TIMEOUT_I3 extends RPCStatus(INTERNAL_ERROR, HttpStatus.GatewayTimeout_504)

  /** Request cannot be processed within deadline - client may retry */
  case object DEADLINE_EXCEEDED_I4 extends RPCStatus(INTERNAL_ERROR, HttpStatus.GatewayTimeout_504)

  /** Request interrupted at service */
  case object INTERRUPTED_I5 extends RPCStatus(INTERNAL_ERROR, HttpStatus.InternalServerError_500)

  /** Service starting up - retry after delay */
  case object SERVICE_STARTING_UP_I6
      extends RPCStatus(INTERNAL_ERROR, HttpStatus.ServiceUnavailable_503)

  /** Service shutting down */
  case object SERVICE_SHUTTING_DOWN_I7
      extends RPCStatus(INTERNAL_ERROR, HttpStatus.ServiceUnavailable_503)

  /** Data loss or corruption */
  case object DATA_LOSS_I8 extends RPCStatus(INTERNAL_ERROR, HttpStatus.InternalServerError_500)

  // ============ Resource Exhausted (429) ============

  /** Resource for completing request is insufficient */
  case object RESOURCE_EXHAUSTED_R0
      extends RPCStatus(RESOURCE_EXHAUSTED, HttpStatus.TooManyRequests_429)

  /** Service experiencing insufficient memory */
  case object OUT_OF_MEMORY_R1 extends RPCStatus(RESOURCE_EXHAUSTED, HttpStatus.TooManyRequests_429)

  /** Too many requests - retry after delay */
  case object EXCEEDED_RATE_LIMIT_R2
      extends RPCStatus(RESOURCE_EXHAUSTED, HttpStatus.TooManyRequests_429)

  /** CPU usage limit reached */
  case object EXCEEDED_CPU_LIMIT_R3
      extends RPCStatus(RESOURCE_EXHAUSTED, HttpStatus.TooManyRequests_429)

  /** Memory usage limit reached */
  case object EXCEEDED_MEMORY_LIMIT_R4
      extends RPCStatus(RESOURCE_EXHAUSTED, HttpStatus.TooManyRequests_429)

  /** Running time limit reached */
  case object EXCEEDED_TIME_LIMIT_R5
      extends RPCStatus(RESOURCE_EXHAUSTED, HttpStatus.TooManyRequests_429)

  /** Data size limit reached */
  case object EXCEEDED_DATA_SIZE_LIMIT_R6
      extends RPCStatus(RESOURCE_EXHAUSTED, HttpStatus.TooManyRequests_429)

  /** Storage size limit reached */
  case object EXCEEDED_STORAGE_LIMIT_R7
      extends RPCStatus(RESOURCE_EXHAUSTED, HttpStatus.TooManyRequests_429)

  /** Budget exhausted */
  case object EXCEEDED_BUDGET_R8
      extends RPCStatus(RESOURCE_EXHAUSTED, HttpStatus.TooManyRequests_429)

  /**
    * Extract error code from the status name suffix
    */
  private def extractErrorCode(name: String): Int =
    val separatorPos = name.lastIndexOf("_")
    if separatorPos == -1 then
      throw AssertionError(s"Invalid code name ${name}. It must end with (S/U/I/R)[0-9]+")

    val suffix = name.substring(separatorPos + 1)
    if suffix.length < 2 then
      throw AssertionError(s"Invalid code suffix ${name}. It must have a suffix (S/U/I/R)[0-9]+")

    try
      val errorCode  = suffix.substring(1).toInt
      val statusType = RPCStatusType.ofPrefix(suffix.charAt(0))
      statusType.minCode + errorCode
    catch
      case e: NumberFormatException =>
        throw AssertionError(
          s"Invalid code suffix ${name}. It must have an integer suffix: ${e.getMessage}"
        )

end RPCStatus

/**
  * Base class for RPC status codes
  */
sealed abstract class RPCStatus(val statusType: RPCStatusType, val httpStatus: HttpStatus):
  import RPCStatus.*

  /**
    * Integer-based error code derived from the name
    */
  lazy val code: Int = extractErrorCode(name)

  /**
    * Status name (e.g., "INVALID_REQUEST_U1")
    */
  def name: String = toString

  def isSuccess: Boolean = statusType == RPCStatusType.SUCCESS
  def isFailure: Boolean = !isSuccess

  /**
    * Whether stack traces should be reported for this status. Auth-related errors suppress stack
    * traces by default.
    */
  def shouldReportStackTrace: Boolean =
    this match
      case RPCStatus.UNAUTHENTICATED_U13 | RPCStatus.PERMISSION_DENIED_U14 =>
        false
      case _ =>
        true

  /**
    * Create a new RPCException with this status
    */
  def newException(
      message: String,
      cause: Throwable = null,
      appErrorCode: Option[Int] = None
  ): RPCException = RPCException(
    status = this,
    message = message,
    cause = Option(cause),
    appErrorCode = appErrorCode
  )

end RPCStatus
