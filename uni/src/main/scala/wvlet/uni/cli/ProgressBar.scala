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

import java.io.PrintStream

/**
  * Configuration for a terminal progress bar.
  *
  * Usage:
  * {{{
  * import wvlet.uni.cli.*
  *
  * // Basic progress bar
  * val bar = ProgressBar(total = 100).start()
  * for i <- 1 to 100 do
  *   Thread.sleep(50)
  *   bar.increment()
  * bar.finish()
  *
  * // Custom progress bar
  * val bar = ProgressBar(total = 1000)
  *   .withWidth(30)
  *   .withPrefix("Downloading: ")
  *   .withShowCount(true)
  *   .withColor(Tint.cyan)
  *   .start()
  *
  * bar.update(500)
  * bar.finish()
  * }}}
  */
case class ProgressBar(
    total: Long,
    width: Int = 40,
    complete: String = "█",
    incomplete: String = "░",
    prefix: String = "",
    suffix: String = "",
    showPercent: Boolean = true,
    showCount: Boolean = false,
    color: Tint = Tint.green,
    stream: PrintStream = System.err,
    enabled: Boolean = true,
    hideCursor: Boolean = true
):
  require(total > 0, "Total must be greater than 0")
  require(width > 0, "Width must be greater than 0")

  /**
    * Start the progress bar.
    */
  def start(): RunningProgressBar = RunningProgressBar.create(this)

  // Builder methods
  def withTotal(total: Long): ProgressBar          = this.copy(total = total)
  def withWidth(width: Int): ProgressBar           = this.copy(width = width)
  def withComplete(char: String): ProgressBar      = this.copy(complete = char)
  def withIncomplete(char: String): ProgressBar    = this.copy(incomplete = char)
  def withPrefix(prefix: String): ProgressBar      = this.copy(prefix = prefix)
  def withSuffix(suffix: String): ProgressBar      = this.copy(suffix = suffix)
  def withShowPercent(show: Boolean): ProgressBar  = this.copy(showPercent = show)
  def withShowCount(show: Boolean): ProgressBar    = this.copy(showCount = show)
  def withColor(color: Tint): ProgressBar         = this.copy(color = color)
  def withStream(stream: PrintStream): ProgressBar = this.copy(stream = stream)
  def withEnabled(enabled: Boolean): ProgressBar   = this.copy(enabled = enabled)
  def withHideCursor(hide: Boolean): ProgressBar   = this.copy(hideCursor = hide)

  /**
    * Disable the progress bar (useful for non-interactive environments).
    */
  def noProgressBar: ProgressBar = this.copy(enabled = false)

  /**
    * Disable color output.
    */
  def noColor: ProgressBar = this.copy(color = Tint().noColor)

end ProgressBar

/**
  * Trait for a running progress bar. Platform-specific implementations handle the rendering.
  */
trait RunningProgressBar:
  /**
    * Get the current progress value.
    */
  def current: Long

  /**
    * Update progress to the specified value.
    */
  def update(value: Long): Unit

  /**
    * Increment progress by the specified amount.
    */
  def increment(amount: Long = 1): Unit

  /**
    * Finish the progress bar with success.
    */
  def finish(): Unit

  /**
    * Finish the progress bar with failure.
    */
  def fail(): Unit

  /**
    * Returns true if the progress bar is complete.
    */
  def isComplete: Boolean

end RunningProgressBar

/**
  * Platform-specific factory for creating RunningProgressBar instances.
  */
object RunningProgressBar:
  /**
    * Create a platform-specific RunningProgressBar.
    */
  def create(config: ProgressBar): RunningProgressBar = RunningProgressBarImpl.create(config)
