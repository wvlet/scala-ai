package wvlet.ai.util.log

import java.io.PrintStream

/**
  */
private[log] trait LogEnvBase:
  def initLogManager(): Unit = {
    // do nothing by default
  }

  def isScalaJS: Boolean
  def isScalaNative: Boolean

  def defaultLogLevel: LogLevel
  def defaultHandler: java.util.logging.Handler
  def defaultConsoleOutput: PrintStream

  /**
    * @param cl
    * @return
    */
  def getLoggerName(cl: Class[?]): String

end LogEnvBase
