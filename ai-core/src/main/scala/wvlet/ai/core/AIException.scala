package wvlet.ai.core

case class AIException(statusCode: StatusCode, message: String, cause: Throwable = null)
    extends Exception(message, cause)
