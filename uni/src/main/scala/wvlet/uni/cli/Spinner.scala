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
  * Configuration for a terminal spinner.
  *
  * Usage:
  * {{{
  * import wvlet.uni.cli.*
  *
  * // Basic spinner
  * val spinner = Spinner().start("Loading...")
  * Thread.sleep(2000)
  * spinner.succeed("Done!")
  *
  * // Custom spinner
  * val spinner = Spinner()
  *   .withText("Processing")
  *   .withSpinner(SpinnerStyle.Dots)
  *   .withColor(Tint.cyan)
  *   .start()
  *
  * spinner.text = "Still processing..."
  * spinner.succeed("Complete")
  * }}}
  */
case class Spinner(
    text: String = "",
    style: SpinnerStyle = SpinnerStyle.Default,
    color: Tint = Tint.cyan,
    stream: PrintStream = System.err,
    enabled: Boolean = true,
    hideCursor: Boolean = true
):
  /**
    * Start the spinner with the current text.
    */
  def start(): RunningSpinner = RunningSpinner.create(this)

  /**
    * Start the spinner with the specified text.
    */
  def start(text: String): RunningSpinner = this.withText(text).start()

  // Builder methods
  def withText(text: String): Spinner           = this.copy(text = text)
  def withSpinner(style: SpinnerStyle): Spinner = this.copy(style = style)
  def withStyle(style: SpinnerStyle): Spinner   = this.copy(style = style)
  def withColor(color: Tint): Spinner           = this.copy(color = color)
  def withStream(stream: PrintStream): Spinner  = this.copy(stream = stream)
  def withEnabled(enabled: Boolean): Spinner    = this.copy(enabled = enabled)
  def withHideCursor(hide: Boolean): Spinner    = this.copy(hideCursor = hide)

  /**
    * Disable the spinner (useful for non-interactive environments).
    */
  def noSpinner: Spinner = this.copy(enabled = false)

  /**
    * Disable color output.
    */
  def noColor: Spinner = this.copy(color = Tint().noColor)

end Spinner

/**
  * Trait for a running spinner. Platform-specific implementations handle the animation.
  */
trait RunningSpinner:
  /**
    * Get the current text.
    */
  def text: String

  /**
    * Set new text to display.
    */
  def text_=(newText: String): Unit

  /**
    * Stop the spinner and show success symbol with optional text.
    */
  def succeed(text: String = ""): Unit

  /**
    * Stop the spinner and show error symbol with optional text.
    */
  def fail(text: String = ""): Unit

  /**
    * Stop the spinner and show warning symbol with optional text.
    */
  def warn(text: String = ""): Unit

  /**
    * Stop the spinner and show info symbol with optional text.
    */
  def info(text: String = ""): Unit

  /**
    * Stop the spinner without showing any symbol.
    */
  def stop(): Unit

  /**
    * Stop the spinner and clear the line.
    */
  def clear(): Unit

  /**
    * Returns true if the spinner is currently running.
    */
  def isSpinning: Boolean

end RunningSpinner

/**
  * Platform-specific factory for creating RunningSpinner instances.
  */
object RunningSpinner:
  /**
    * Create a platform-specific RunningSpinner. This method is implemented differently for JVM, JS,
    * and Native.
    */
  def create(config: Spinner): RunningSpinner = RunningSpinnerImpl.create(config)
