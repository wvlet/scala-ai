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
package wvlet.ai.core.log

import java.io.{PrintWriter, StringWriter}
import java.util.logging.Formatter
import java.util.regex.Pattern
import java.util.logging as jl
import LogLevel.{DEBUG, ERROR, INFO, TRACE, WARN}

/**
  * To implement your own log formatter, implement this formatLog(r: LogRecord) method
  */
trait LogFormatter extends Formatter:
  def formatLog(r: LogRecord): String

  override def format(record: jl.LogRecord): String =
    record match
      case lr: LogRecord =>
        formatLog(lr)
      case _ =>
        formatLog(LogRecord(record))

object LogFormatter extends AnsiColorPalette:
  import LogTimestampFormatter.*

  def currentThreadName: String = Thread.currentThread().getName

  private val testFrameworkFilter = Pattern.compile(
    """\s+at (sbt\.|org\.scalatest\.|wvlet\.airspec\.).*"""
  )

  val DEFAULT_STACKTRACE_FILTER: String => Boolean =
    (line: String) => !testFrameworkFilter.matcher(line).matches()

  private var stackTraceFilter: String => Boolean = DEFAULT_STACKTRACE_FILTER

  /**
    * Set stack trace line filter
    *
    * @param filter
    */
  def setStackTraceFilter(filter: String => Boolean): Unit = stackTraceFilter = filter

  def formatStacktrace(e: Throwable): String =
    e match
      case null =>
        // Exception cause can be null
        ""
      case _ =>
        val trace = new StringWriter()
        e.printStackTrace(new PrintWriter(trace))
        val stackTraceLines = trace.toString.split("\n")
        val filtered = stackTraceLines
          .filter(stackTraceFilter)
          .sliding(2)
          .collect {
            case Array(a, b) if a != b =>
              b
          }

        (stackTraceLines.headOption ++ filtered).mkString("\n")

  def withColor(prefix: String, s: String) = s"${prefix}${s}${Console.RESET}"

  def highlightLog(level: LogLevel, message: String): String =
    val color =
      level match
        case ERROR =>
          Console.RED
        case WARN =>
          Console.YELLOW
        case INFO =>
          Console.CYAN
        case DEBUG =>
          Console.GREEN
        case TRACE =>
          Console.MAGENTA
        case _ =>
          Console.RESET
    withColor(color, message)

  def appendStackTrace(m: String, r: LogRecord, coloring: Boolean = true): String =
    r.cause match
      case Some(ex) if coloring =>
        s"${m}\n${highlightLog(r.level, formatStacktrace(ex))}"
      case Some(ex) =>
        s"${m}\n${formatStacktrace(ex)}"
      case None =>
        m

  object TSVLogFormatter extends LogFormatter:
    override def formatLog(record: LogRecord): String =
      val s = Seq.newBuilder[String]
      s += formatTimestampWithoutSpace(record.getMillis())
      s += record.level.toString
      s += currentThreadName
      s += record.leafLoggerName
      s += record.getMessage()

      val log = s.result().mkString("\t")
      record.cause match
        case Some(ex) =>
          // Print only the first line of the exception message
          s"${log}\n${formatStacktrace(ex).split("\n").head}"
        case None =>
          log

  /**
    * Simple log formatter that shows only logger name and message
    */
  object SimpleLogFormatter extends LogFormatter:
    override def formatLog(r: LogRecord): String =
      val log =
        s"[${highlightLog(r.level, r.leafLoggerName)}] ${highlightLog(r.level, r.getMessage())}"
      appendStackTrace(log, r)

  /**
    * log format for command-line user clients (without source code location)
    */
  object AppLogFormatter extends LogFormatter:
    override def formatLog(r: LogRecord): String =
      val logTag = highlightLog(r.level, r.level.name)
      val log =
        f"${withColor(Console.BLUE, formatTimestamp(r.getMillis()))} ${logTag}%14s [${withColor(
            Console.WHITE,
            r.leafLoggerName
          )}] ${highlightLog(r.level, r.getMessage())}"
      appendStackTrace(log, r)

  /**
    * log format for debugging source code
    */
  object SourceCodeLogFormatter extends LogFormatter:
    override def formatLog(r: LogRecord): String =
      val loc = r
        .source
        .map(source => s" ${withColor(Console.BLUE, s"- (${source.fileLocation})")}")
        .getOrElse("")

      val logTag = highlightLog(r.level, r.level.name)
      val log =
        f"${withColor(Console.BLUE, formatTimestamp(r.getMillis()))} ${logTag}%14s [${withColor(
            Console.WHITE,
            r.leafLoggerName
          )}] ${highlightLog(r.level, r.getMessage())} ${loc}"
      appendStackTrace(log, r)

  object ThreadLogFormatter extends LogFormatter:
    override def formatLog(r: LogRecord): String =
      val loc = r
        .source
        .map(source => s" ${withColor(Console.BLUE, s"- (${source.fileLocation})")}")
        .getOrElse("")

      val logTag = highlightLog(r.level, r.level.name)
      val log =
        f"${withColor(Console.BLUE, formatTimestamp(r.getMillis()))} [${withColor(
            BRIGHT_BLUE,
            currentThreadName
          )}] ${logTag}%14s [${withColor(Console.WHITE, r.leafLoggerName)}] ${highlightLog(
            r.level,
            r.getMessage()
          )} ${loc}"
      appendStackTrace(log, r)

  /**
    * log format for debugging source code without using ANSI colors
    */
  object PlainSourceCodeLogFormatter extends LogFormatter:
    override def formatLog(r: LogRecord): String =
      val loc = r.source.map(source => s" - (${source.fileLocation})").getOrElse("")

      val log =
        f"${formatTimestamp(r.getMillis())} ${r.level.name}%5s [${r.leafLoggerName}] ${r
            .getMessage()} ${loc}"
      appendStackTrace(log, r, coloring = false)

  /**
    * Enable source code links in the run/debug console of IntelliJ
    */
  object IntelliJLogFormatter extends LogFormatter:
    override def formatLog(r: LogRecord): String =
      val loc = r
        .source
        .map(source =>
          s" ${withColor(Console.BLUE, s"- ${r.getLoggerName()}(${source.fileLocation})")}"
        )
        .getOrElse("")

      val log =
        s"[${highlightLog(r.level, r.level.name)}] ${highlightLog(r.level, r.getMessage())}$loc"
      appendStackTrace(log, r)

  /**
    * For formatting log as is.
    */
  object BareFormatter extends LogFormatter:
    override def formatLog(r: LogRecord): String =
      val m = r.getMessage()
      r.cause match
        case Some(ex) =>
          s"${m}\n${formatStacktrace(ex)}"
        case None =>
          m

end LogFormatter
