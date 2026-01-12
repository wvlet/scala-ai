package wvlet.uni.agent.core

case class AIException(statusCode: StatusCode, message: String, cause: Throwable = null)
    extends Exception(message, cause)
