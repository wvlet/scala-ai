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

import wvlet.uni.http.{HttpHeader, Response}
import wvlet.uni.json.JSON
import wvlet.uni.msgpack.spi.MsgPack
import wvlet.uni.weaver.ObjectWeaver

import scala.util.Try

/**
  * RPC exception for reporting errors across RPC boundaries.
  *
  * Create using: `RPCStatus.INVALID_REQUEST_U1.newException("error message")`
  */
case class RPCException(
    status: RPCStatus = RPCStatus.INTERNAL_ERROR_I0,
    message: String = "",
    cause: Option[Throwable] = None,
    appErrorCode: Option[Int] = None
) extends Exception(s"[${status}] ${message}", cause.orNull):

  private var includeStackTrace: Option[Boolean] = None

  /**
    * Suppress stack trace in the RPC error response
    */
  def noStackTrace: RPCException =
    includeStackTrace = Some(false)
    this

  /**
    * Whether stack trace should be included in the error response
    */
  def shouldReportStackTrace: Boolean =
    includeStackTrace match
      case Some(b) =>
        b
      case None =>
        status.shouldReportStackTrace

  /**
    * Convert to serializable error message
    */
  def toMessage: RPCErrorMessage = RPCErrorMessage(
    code = status.code,
    codeName = status.name,
    message = message,
    appErrorCode = appErrorCode
  )

  /**
    * Serialize to JSON
    */
  def toJson: String = toMessage.toJson

  /**
    * Serialize to MessagePack
    */
  def toMsgPack: MsgPack = toMessage.toMsgPack

  /**
    * Convert to HTTP response with RPC status header
    */
  def toResponse: Response = Response(status.httpStatus)
    .addHeader(HttpHeader.xRPCStatus, status.code.toString)
    .withJsonContent(toJson)

end RPCException

/**
  * Serializable RPC error message model
  */
case class RPCErrorMessage(
    code: Int = RPCStatus.UNKNOWN_I1.code,
    codeName: String = RPCStatus.UNKNOWN_I1.name,
    message: String = "",
    appErrorCode: Option[Int] = None
):
  def toJson: String     = RPCErrorMessage.weaver.toJson(this)
  def toMsgPack: MsgPack = RPCErrorMessage.weaver.weave(this)

object RPCErrorMessage:
  given weaver: ObjectWeaver[RPCErrorMessage] = ObjectWeaver.derived[RPCErrorMessage]

  def fromJson(json: String): RPCErrorMessage        = weaver.fromJson(json)
  def fromMsgPack(msgpack: MsgPack): RPCErrorMessage = weaver.unweave(msgpack)

end RPCErrorMessage

object RPCException:

  private def fromRPCErrorMessage(m: RPCErrorMessage): RPCException = RPCException(
    status = RPCStatus.ofCode(m.code),
    message = m.message,
    cause = None,
    appErrorCode = m.appErrorCode
  )

  def fromJson(json: String): RPCException =
    val m = RPCErrorMessage.fromJson(json)
    fromRPCErrorMessage(m)

  def fromMsgPack(msgpack: MsgPack): RPCException =
    val m = RPCErrorMessage.fromMsgPack(msgpack)
    fromRPCErrorMessage(m)

  /**
    * Extract RPCException from HTTP response
    */
  def fromResponse(response: Response): RPCException =
    val rpcStatusOpt = response.header(HttpHeader.xRPCStatus).flatMap(x => Try(x.toInt).toOption)
    rpcStatusOpt match
      case Some(rpcStatusCode) =>
        try
          val contentOpt = response.contentAsString
          if contentOpt.isEmpty || contentOpt.get.isBlank then
            val status = RPCStatus.ofCode(rpcStatusCode)
            status.newException(status.name)
          else
            RPCException.fromJson(contentOpt.get)
        catch
          case e: Throwable =>
            RPCStatus
              .ofCode(rpcStatusCode)
              .newException(s"Failed to parse RPC error details: ${e.getMessage}", e)
      case None =>
        // No RPC status header - derive from HTTP status
        val rpcStatus = RPCStatus.fromHttpStatus(response.status)
        rpcStatus.newException(response.contentAsString.getOrElse(s"RPC error: ${response.status}"))

end RPCException
