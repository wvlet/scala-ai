package wvlet.ai.agent.mcp

import wvlet.airframe.codec.MessageCodec

/**
  * JSON-RPC 2.0 protocol messages for MCP communication.
  *
  * Based on: https://www.jsonrpc.org/specification
  */
object JsonRpc:

  /**
    * JSON-RPC request message
    */
  case class Request(
      jsonrpc: String = "2.0",
      id: Option[Any],
      method: String,
      params: Option[Map[String, Any]] = None
  ):
    def withParams(params: Map[String, Any]): Request = copy(params = Some(params))
    def noParams: Request                             = copy(params = None)

  /**
    * JSON-RPC response message
    */
  case class Response(
      jsonrpc: String = "2.0",
      id: Option[Any],
      result: Option[Any] = None,
      error: Option[ErrorObject] = None
  )

  /**
    * JSON-RPC error object
    */
  case class ErrorObject(code: Int, message: String, data: Option[Any] = None)

  /**
    * JSON-RPC notification (request without id)
    */
  case class Notification(
      jsonrpc: String = "2.0",
      method: String,
      params: Option[Map[String, Any]] = None
  ):
    def withParams(params: Map[String, Any]): Notification = copy(params = Some(params))
    def noParams: Notification                             = copy(params = None)

  /**
    * Standard JSON-RPC error codes
    */
  object ErrorCode:
    val ParseError     = -32700
    val InvalidRequest = -32600
    val MethodNotFound = -32601
    val InvalidParams  = -32602
    val InternalError  = -32603

  /**
    * Parse a JSON string into a JSON-RPC message
    */
  def parse(json: String): Either[ErrorObject, Request | Response | Notification] =
    try
      val parsed = MessageCodec.fromJson[Map[String, Any]](json)
      // Check if it has an id field to distinguish request/response from notification
      val hasId     = parsed.contains("id")
      val hasMethod = parsed.contains("method")
      val hasResult = parsed.contains("result")
      val hasError  = parsed.contains("error")

      if hasMethod && hasId then
        val codec = MessageCodec.of[Request]
        Right(codec.fromMap(parsed))
      else if hasMethod && !hasId then
        val codec = MessageCodec.of[Notification]
        Right(codec.fromMap(parsed))
      else if hasResult || hasError then
        val codec = MessageCodec.of[Response]
        Right(codec.fromMap(parsed))
      else
        Left(ErrorObject(ErrorCode.InvalidRequest, "Invalid JSON-RPC message format"))
    catch
      case e: Exception =>
        Left(ErrorObject(ErrorCode.ParseError, s"Failed to parse JSON: ${e.getMessage}"))

end JsonRpc
