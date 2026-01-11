package wvlet.uni.core.log

import wvlet.uni.core.log.LogLevel
import wvlet.uni.core.log.Logger

import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.annotation.JSExportTopLevel

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
