package wvlet.ai.core

enum StatusType:
  // Success status
  case Success
  // User or query errors, which is not retryable
  case UserError
  // Internal errors, which is usually retryable
  case InternalError
  // Not enough resources to complete the task. Users can retry after the resource is available
  case ResourceExhausted


enum StatusCode(statusType):
  case OK extends StatusCode(StatusType.Success)
  // Used for successful exit by throwing an Exception
  case EXIT_SUCCESSFULLY extends StatusCode(StatusType.Success)

  // User errors
  case SYNTAX_ERROR extends StatusCode(StatusType.UserError)
  case TEST_FAILED extends StatusCode(StatusType.UserError)

  // Internal errors
  case INTERNAL_ERROR extends StatusCode(StatusType.InternalError)

  // Resource errors
  case RESOURCE_EXHAUSTED extends StatusCode(StatusType.ResourceExhausted)

  def isUserError: Boolean = statusType == StatusType.UserError
  def isInternalError: Boolean = statusType == StatusType.InternalError
  def isSuccess: Boolean = statusType == StatusType.Success
  def isResourceExhausted: Boolean = statusType == StatusType.ResourceExhausted
  def name: String = this.toString

  def newException(msg: String): AIException = AIException(this, msg)
  def newException(msg: String, cause: Throwable): AIException = AIException(
    this,
    msg,
    cause
  )
