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

import scala.scalajs.js
import scala.scalajs.js.Dynamic.global as g
import scala.scalajs.js.timers.*
import scala.compiletime.uninitialized

/**
  * Scala.js-specific RunningSpinner implementation using setInterval.
  */
private[cli] object RunningSpinnerImpl:
  def create(config: Spinner): RunningSpinner =
    if config.enabled && CliEnv.isInteractive then
      JsRunningSpinner(config)
    else
      DisabledSpinner(config)

/**
  * Disabled spinner that just logs text without animation.
  */
private class DisabledSpinner(config: Spinner) extends RunningSpinner:
  private var currentText: String = config.text
  private var running: Boolean    = true

  // Print initial text if any
  if currentText.nonEmpty then
    println(currentText)

  override def text: String                  = currentText
  override def text_=(newText: String): Unit = currentText = newText

  override def succeed(text: String): Unit =
    val finalText =
      if text.nonEmpty then
        text
      else
        currentText
    println(s"${Symbols.successColored} ${finalText}")
    running = false

  override def fail(text: String): Unit =
    val finalText =
      if text.nonEmpty then
        text
      else
        currentText
    println(s"${Symbols.errorColored} ${finalText}")
    running = false

  override def warn(text: String): Unit =
    val finalText =
      if text.nonEmpty then
        text
      else
        currentText
    println(s"${Symbols.warningColored} ${finalText}")
    running = false

  override def info(text: String): Unit =
    val finalText =
      if text.nonEmpty then
        text
      else
        currentText
    println(s"${Symbols.infoColored} ${finalText}")
    running = false

  override def stop(): Unit        = running = false
  override def clear(): Unit       = running = false
  override def isSpinning: Boolean = running

end DisabledSpinner

/**
  * JS-specific running spinner with animation (Node.js only).
  */
private class JsRunningSpinner(config: Spinner) extends RunningSpinner:
  private var currentText: String               = config.text
  private var running: Boolean                  = true
  private var frameIndex: Int                   = 0
  private val frames                            = config.style.frames
  private var intervalHandle: SetIntervalHandle = uninitialized

  // Start the spinner
  init()

  private def init(): Unit =
    if config.hideCursor then
      hideCursor()

    intervalHandle =
      setInterval(config.style.interval.toDouble) {
        render()
      }

  private def hideCursor(): Unit =
    if js.typeOf(g.process) != "undefined" then
      g.process.stderr.write("\u001b[?25l")

  private def showCursor(): Unit =
    if js.typeOf(g.process) != "undefined" then
      g.process.stderr.write("\u001b[?25h")

  private def write(s: String): Unit =
    if js.typeOf(g.process) != "undefined" then
      g.process.stderr.write(s)

  private def render(): Unit =
    if running then
      val frame = config.color(frames(frameIndex))
      frameIndex = (frameIndex + 1) % frames.size
      val txt  = currentText
      val line =
        if txt.nonEmpty then
          s"${frame} ${txt}"
        else
          frame
      write(s"\r${line}\u001b[K")

  private def stopAnimation(): Unit =
    running = false
    if intervalHandle != null then
      clearInterval(intervalHandle)
    if config.hideCursor then
      showCursor()

  private def finishWithSymbol(symbol: String, finalText: String): Unit =
    stopAnimation()
    val txt =
      if finalText.nonEmpty then
        finalText
      else
        currentText
    val line =
      if txt.nonEmpty then
        s"${symbol} ${txt}"
      else
        symbol
    write(s"\r${line}\u001b[K\n")

  override def text: String                  = currentText
  override def text_=(newText: String): Unit = currentText = newText

  override def succeed(text: String): Unit = finishWithSymbol(Symbols.successColored, text)

  override def fail(text: String): Unit = finishWithSymbol(Symbols.errorColored, text)

  override def warn(text: String): Unit = finishWithSymbol(Symbols.warningColored, text)

  override def info(text: String): Unit = finishWithSymbol(Symbols.infoColored, text)

  override def stop(): Unit =
    stopAnimation()
    if currentText.nonEmpty then
      write(s"\r${currentText}\u001b[K\n")

  override def clear(): Unit =
    stopAnimation()
    write("\r\u001b[K")

  override def isSpinning: Boolean = running

end JsRunningSpinner
