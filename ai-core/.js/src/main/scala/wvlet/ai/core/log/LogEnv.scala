package wvlet.ai.core.log

import wvlet.ai.core.log.LogEnvBase
import wvlet.ai.core.log.LogLevel

import java.io.PrintStream

/**
  */
private[log] object LogEnv extends LogEnvBase:
  override def isScalaJS: Boolean        = true
  override def isScalaNative: Boolean    = false
  override def defaultLogLevel: LogLevel = LogLevel.INFO

  override def defaultConsoleOutput: PrintStream         = Console.out
  override def defaultHandler: java.util.logging.Handler = JSConsoleLogHandler()

  override def getLoggerName(cl: Class[?]): String =
    var name = cl.getName

    // In Scala.js we cannot use cl.getInterfaces to find the actual type
    val pos = name.indexOf("$")
    if pos > 0 then
      // Remove trailing $xxx
      name = name.substring(0, pos)
    name
