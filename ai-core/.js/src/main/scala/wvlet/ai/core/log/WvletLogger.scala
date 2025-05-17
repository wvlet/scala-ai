package wvlet.ai.core.log

import wvlet.ai.core.log.{LogLevel, Logger}

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

/**
  */
@JSExportTopLevel("WvletLogger")
object WvletLogger:
  @JSExport
  def setDefaultLogLevel(level: String): Boolean =
    Logger.setDefaultLogLevel(LogLevel(level))
    true

  @JSExport
  def setLogLevel(loggerName: String, level: String): Boolean =
    Logger(loggerName).setLogLevel(LogLevel(level))
    true
