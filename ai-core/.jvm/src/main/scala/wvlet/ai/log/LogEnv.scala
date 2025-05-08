package wvlet.ai.log

import wvlet.ai.log.LogFormatter.SourceCodeLogFormatter
import wvlet.ai.log.{ConsoleLogHandler, LogEnvBase, LogLevel}

import java.io.PrintStream

/**
  */
private[log] object LogEnv extends LogEnvBase:
  override def isScalaJS: Boolean        = false
  override def isScalaNative: Boolean    = false
  override def defaultLogLevel: LogLevel = LogLevel.INFO

  override val defaultConsoleOutput: PrintStream =
    // Note: In normal circumstances, using System.err here is fine, but
    // System.err can be replaced with other implementations
    // (e.g., airlift.Logging, which is used in Trino https://github.com/airlift/airlift/blob/master/log-manager/src/main/java/io/airlift/log/Logging.java),
    // If that happens, we may need to create a stderr stream explicitly like this
    // new PrintStream(new FileOutputStream(FileDescriptor.err))

    // Use the standard System.err for sbtn native client
    System.err

  override def defaultHandler: java.util.logging.Handler =
    new ConsoleLogHandler(SourceCodeLogFormatter)

  /**
    * @param cl
    * @return
    */
  override def getLoggerName(cl: Class[?]): String =
    var name = cl.getName

    if name.endsWith("$") then
      // Remove trailing $ of Scala Object name
      name = name.substring(0, name.length - 1)

    // When class is an anonymous trait
    if name.contains("$anon$") then
      val interfaces = cl.getInterfaces
      if interfaces != null && interfaces.length > 0 then
        // Use the first interface name instead of the anonymous name
        name = interfaces(0).getName
    name

end LogEnv
