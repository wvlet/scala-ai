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
  * Preset styles for progress bars.
  *
  * Usage:
  * {{{
  * import wvlet.uni.cli.*
  *
  * val bar = ProgressBarStyle.Shaded(total = 100).start()
  * bar.increment()
  * bar.finish()
  * }}}
  */
object ProgressBarStyle:
  /**
    * Default progress bar with solid block characters. Output: [████████░░░░░░░░░░░░] 50%
    */
  def Default(total: Long): ProgressBar = ProgressBar(total = total)
    .withComplete("█")
    .withIncomplete("░")
    .withColor(Chalk.green)

  /**
    * Shaded progress bar with gradient block characters. Output: [▓▓▓▓▓▓▓▓░░░░░░░░░░░░] 50%
    */
  def Shaded(total: Long): ProgressBar = ProgressBar(total = total)
    .withComplete("▓")
    .withIncomplete("░")
    .withColor(Chalk.cyan)

  /**
    * Block progress bar with filled/empty squares. Output: [■■■■■■■■□□□□□□□□□□□□] 50%
    */
  def Block(total: Long): ProgressBar = ProgressBar(total = total)
    .withComplete("■")
    .withIncomplete("□")
    .withColor(Chalk.blue)

  /**
    * Arrow progress bar. ASCII-compatible. Output: [========>-----------] 50%
    */
  def Arrow(total: Long): ProgressBar = ProgressBar(total = total)
    .withComplete("=")
    .withIncomplete("-")
    .withColor(Chalk.yellow)

  /**
    * Hash progress bar. ASCII-compatible. Output: [##########..........] 50%
    */
  def Hash(total: Long): ProgressBar = ProgressBar(total = total)
    .withComplete("#")
    .withIncomplete(".")
    .withColor(Chalk.magenta)

  /**
    * Classic progress bar with percentage and count. Output: Downloading: [████░░░░] 50% (50/100)
    */
  def Classic(total: Long, prefix: String = ""): ProgressBar = ProgressBar(total = total)
    .withComplete("█")
    .withIncomplete("░")
    .withPrefix(prefix)
    .withShowPercent(true)
    .withShowCount(true)
    .withWidth(30)
    .withColor(Chalk.green)

  /**
    * Minimal progress bar without percentage. Output: [████████░░░░░░░░░░░░]
    */
  def Minimal(total: Long): ProgressBar = ProgressBar(total = total)
    .withComplete("█")
    .withIncomplete("░")
    .withShowPercent(false)
    .withColor(Chalk.white)

  /**
    * Colorful progress bar with rainbow gradient.
    */
  def Rainbow(total: Long): ProgressBar = ProgressBar(total = total)
    .withComplete("█")
    .withIncomplete("░")
    .withColor(Chalk.brightMagenta)

end ProgressBarStyle
