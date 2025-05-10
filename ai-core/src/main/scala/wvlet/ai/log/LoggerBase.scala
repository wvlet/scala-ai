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
package wvlet.ai.log

import wvlet.ai.util.SourceCode

trait LoggerBase:
  self: Logger =>

  inline def error(inline message: Any): Unit =
    if self.isEnabled(LogLevel.ERROR) then
      self.log(LogLevel.ERROR, SourceCode(), message)

  inline def warn(inline message: Any): Unit =
    if self.isEnabled(LogLevel.WARN) then
      self.log(LogLevel.WARN, SourceCode(), message)

  inline def info(inline message: Any): Unit =
    if self.isEnabled(LogLevel.INFO) then
      self.log(LogLevel.INFO, SourceCode(), message)

  inline def debug(inline message: Any): Unit =
    if self.isEnabled(LogLevel.DEBUG) then
      self.log(LogLevel.DEBUG, SourceCode(), message)

  inline def trace(inline message: Any): Unit =
    if self.isEnabled(LogLevel.TRACE) then
      self.log(LogLevel.TRACE, SourceCode(), message)

  inline def error(inline message: Any, inline cause: Throwable): Unit =
    if self.isEnabled(LogLevel.ERROR) then
      self.logWithCause(LogLevel.ERROR, SourceCode(), message, cause)

  inline def warn(inline message: Any, inline cause: Throwable): Unit =
    if self.isEnabled(LogLevel.WARN) then
      self.logWithCause(LogLevel.WARN, SourceCode(), message, cause)

  inline def info(inline message: Any, inline cause: Throwable): Unit =
    if self.isEnabled(LogLevel.INFO) then
      self.logWithCause(LogLevel.INFO, SourceCode(), message, cause)

  inline def debug(inline message: Any, inline cause: Throwable): Unit =
    if self.isEnabled(LogLevel.DEBUG) then
      self.logWithCause(LogLevel.DEBUG, SourceCode(), message, cause)

  inline def trace(inline message: Any, inline cause: Throwable): Unit =
    if self.isEnabled(LogLevel.TRACE) then
      self.logWithCause(LogLevel.TRACE, SourceCode(), message, cause)

end LoggerBase

/**
  */
trait LoggingMethods extends Serializable:
  protected def logger: Logger

  inline protected def error(inline message: Any): Unit =
    if logger.isEnabled(LogLevel.ERROR) then
      logger.log(LogLevel.ERROR, SourceCode(), message)

  inline protected def warn(inline message: Any): Unit =
    if logger.isEnabled(LogLevel.WARN) then
      logger.log(LogLevel.WARN, SourceCode(), message)

  inline protected def info(inline message: Any): Unit =
    if logger.isEnabled(LogLevel.INFO) then
      logger.log(LogLevel.INFO, SourceCode(), message)

  inline protected def debug(inline message: Any): Unit =
    if logger.isEnabled(LogLevel.DEBUG) then
      logger.log(LogLevel.DEBUG, SourceCode(), message)

  inline protected def trace(inline message: Any): Unit =
    if logger.isEnabled(LogLevel.TRACE) then
      logger.log(LogLevel.TRACE, SourceCode(), message)

  inline protected def logAt(inline logLevel: LogLevel, inline message: Any): Unit =
    if logger.isEnabled(logLevel) then
      logger.log(logLevel, SourceCode(), message)

  inline protected def error(inline message: Any, inline cause: Throwable): Unit =
    if logger.isEnabled(LogLevel.ERROR) then
      logger.logWithCause(LogLevel.ERROR, SourceCode(), message, cause)

  inline protected def warn(inline message: Any, inline cause: Throwable): Unit =
    if logger.isEnabled(LogLevel.WARN) then
      logger.logWithCause(LogLevel.WARN, SourceCode(), message, cause)

  inline protected def info(inline message: Any, inline cause: Throwable): Unit =
    if logger.isEnabled(LogLevel.INFO) then
      logger.logWithCause(LogLevel.INFO, SourceCode(), message, cause)

  inline protected def debug(inline message: Any, inline cause: Throwable): Unit =
    if logger.isEnabled(LogLevel.DEBUG) then
      logger.logWithCause(LogLevel.DEBUG, SourceCode(), message, cause)

  inline protected def trace(inline message: Any, inline cause: Throwable): Unit =
    if logger.isEnabled(LogLevel.TRACE) then
      logger.logWithCause(LogLevel.TRACE, SourceCode(), message, cause)

end LoggingMethods
