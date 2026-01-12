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
package wvlet.uni.cli

/**
  * Status symbols for terminal output. Provides platform-aware symbols with fallbacks for Windows.
  */
object Symbols:
  // Unicode symbols
  private val unicodeSuccess    = "✔"
  private val unicodeError      = "✖"
  private val unicodeWarning    = "⚠"
  private val unicodeInfo       = "ℹ"
  private val unicodePointer    = "❯"
  private val unicodeBullet     = "●"
  private val unicodeStar       = "★"
  private val unicodeArrowRight = "→"
  private val unicodeArrowLeft  = "←"
  private val unicodeArrowUp    = "↑"
  private val unicodeArrowDown  = "↓"

  // ASCII fallbacks for Windows
  private val asciiSuccess    = "√"
  private val asciiError      = "×"
  private val asciiWarning    = "!!"
  private val asciiInfo       = "i"
  private val asciiPointer    = ">"
  private val asciiBullet     = "*"
  private val asciiStar       = "*"
  private val asciiArrowRight = "->"
  private val asciiArrowLeft  = "<-"
  private val asciiArrowUp    = "^"
  private val asciiArrowDown  = "v"

  /**
    * Returns true if Unicode symbols should be used (non-Windows or Windows with Unicode support).
    */
  private def useUnicode: Boolean = !CliEnv.isWindows

  /**
    * Success symbol (✔ or √).
    */
  def success: String =
    if useUnicode then
      unicodeSuccess
    else
      asciiSuccess

  /**
    * Error symbol (✖ or ×).
    */
  def error: String =
    if useUnicode then
      unicodeError
    else
      asciiError

  /**
    * Warning symbol (⚠ or !!).
    */
  def warning: String =
    if useUnicode then
      unicodeWarning
    else
      asciiWarning

  /**
    * Info symbol (ℹ or i).
    */
  def info: String =
    if useUnicode then
      unicodeInfo
    else
      asciiInfo

  /**
    * Pointer symbol (❯ or >).
    */
  def pointer: String =
    if useUnicode then
      unicodePointer
    else
      asciiPointer

  /**
    * Bullet symbol (● or *).
    */
  def bullet: String =
    if useUnicode then
      unicodeBullet
    else
      asciiBullet

  /**
    * Star symbol (★ or *).
    */
  def star: String =
    if useUnicode then
      unicodeStar
    else
      asciiStar

  /**
    * Right arrow symbol (→ or ->).
    */
  def arrowRight: String =
    if useUnicode then
      unicodeArrowRight
    else
      asciiArrowRight

  /**
    * Left arrow symbol (← or <-).
    */
  def arrowLeft: String =
    if useUnicode then
      unicodeArrowLeft
    else
      asciiArrowLeft

  /**
    * Up arrow symbol (↑ or ^).
    */
  def arrowUp: String =
    if useUnicode then
      unicodeArrowUp
    else
      asciiArrowUp

  /**
    * Down arrow symbol (↓ or v).
    */
  def arrowDown: String =
    if useUnicode then
      unicodeArrowDown
    else
      asciiArrowDown

  /**
    * Colored success symbol.
    */
  def successColored: String = Chalk.green(success)

  /**
    * Colored error symbol.
    */
  def errorColored: String = Chalk.red(error)

  /**
    * Colored warning symbol.
    */
  def warningColored: String = Chalk.yellow(warning)

  /**
    * Colored info symbol.
    */
  def infoColored: String = Chalk.blue(info)

end Symbols
