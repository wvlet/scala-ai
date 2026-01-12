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

/**
  * Scala.js-specific RunningProgressBar implementation.
  */
private[cli] object RunningProgressBarImpl:
  def create(config: ProgressBar): RunningProgressBar =
    if config.enabled && CliEnv.isInteractive then
      JsRunningProgressBar(config)
    else
      DisabledProgressBar(config)

/**
  * Disabled progress bar that doesn't render anything.
  */
private class DisabledProgressBar(config: ProgressBar) extends RunningProgressBar:
  private var currentValue: Long = 0

  override def current: Long = currentValue

  override def update(value: Long): Unit = currentValue = Math.min(value, config.total)

  override def increment(amount: Long): Unit =
    currentValue = Math.min(currentValue + amount, config.total)

  override def finish(): Unit = currentValue = config.total

  override def fail(): Unit = ()

  override def isComplete: Boolean = currentValue >= config.total

/**
  * JS-specific running progress bar with rendering (Node.js only).
  */
private class JsRunningProgressBar(config: ProgressBar) extends RunningProgressBar:
  private var currentValue: Long = 0
  private var finished: Boolean  = false

  // Start
  init()

  private def init(): Unit =
    if config.hideCursor then
      hideCursor()
    render()

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
    if finished then
      return

    val value       = currentValue
    val percent     = (value.toDouble / config.total * 100).toInt
    val filledWidth = (value.toDouble / config.total * config.width).toInt
    val emptyWidth  = config.width - filledWidth

    val bar = StringBuilder()
    bar.append("\r")
    bar.append(config.prefix)
    bar.append("[")
    bar.append(config.color(config.complete * filledWidth))
    bar.append(config.incomplete * emptyWidth)
    bar.append("]")

    if config.showPercent then
      val percentStr = f"${percent}%3d"
      bar.append(s" ${percentStr}%")

    if config.showCount then
      bar.append(s" (${value}/${config.total})")

    bar.append(config.suffix)
    bar.append("\u001b[K")

    write(bar.toString)

  override def current: Long = currentValue

  override def update(value: Long): Unit =
    currentValue = Math.min(value, config.total)
    render()

  override def increment(amount: Long): Unit =
    currentValue = Math.min(currentValue + amount, config.total)
    render()

  override def finish(): Unit =
    currentValue = config.total
    render()
    finished = true
    if config.hideCursor then
      showCursor()
    write("\n")

  override def fail(): Unit =
    finished = true
    if config.hideCursor then
      showCursor()
    write("\n")

  override def isComplete: Boolean = currentValue >= config.total

end JsRunningProgressBar
