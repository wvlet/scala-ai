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

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
  * Scala Native-specific RunningProgressBar implementation.
  */
private[cli] object RunningProgressBarImpl:
  def create(config: ProgressBar): RunningProgressBar =
    if config.enabled && CliEnv.isInteractive then
      NativeRunningProgressBar(config)
    else
      DisabledProgressBar(config)

/**
  * Disabled progress bar that doesn't render anything.
  */
private class DisabledProgressBar(config: ProgressBar) extends RunningProgressBar:
  private val currentValue = new AtomicLong(0)

  override def current: Long = currentValue.get()

  override def update(value: Long): Unit = currentValue.set(Math.min(value, config.total))

  override def increment(amount: Long): Unit = currentValue.updateAndGet(v =>
    Math.min(v + amount, config.total)
  )

  override def finish(): Unit = currentValue.set(config.total)

  override def fail(): Unit = ()

  override def isComplete: Boolean = currentValue.get() >= config.total

/**
  * Native-specific running progress bar with rendering.
  */
private class NativeRunningProgressBar(config: ProgressBar) extends RunningProgressBar:
  private val currentValue = new AtomicLong(0)
  private val finished     = new AtomicBoolean(false)

  // Start
  init()

  private def init(): Unit =
    if config.hideCursor then
      Terminal.hideCursor(config.stream)
    render()

  private def render(): Unit =
    if finished.get() then
      return

    val value       = currentValue.get()
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
      bar.append(f" ${percent}%3d%%")

    if config.showCount then
      bar.append(s" (${value}/${config.total})")

    bar.append(config.suffix)
    bar.append("\u001b[K") // Clear to end of line

    config.stream.print(bar.toString)
    config.stream.flush()

  override def current: Long = currentValue.get()

  override def update(value: Long): Unit =
    currentValue.set(Math.min(value, config.total))
    render()

  override def increment(amount: Long): Unit =
    currentValue.updateAndGet(v => Math.min(v + amount, config.total))
    render()

  override def finish(): Unit =
    currentValue.set(config.total)
    render()
    finished.set(true)
    if config.hideCursor then
      Terminal.showCursor(config.stream)
    config.stream.println()
    config.stream.flush()

  override def fail(): Unit =
    finished.set(true)
    if config.hideCursor then
      Terminal.showCursor(config.stream)
    config.stream.println()
    config.stream.flush()

  override def isComplete: Boolean = currentValue.get() >= config.total

end NativeRunningProgressBar
