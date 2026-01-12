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
  * Spinner animation styles with customizable frames and intervals.
  *
  * @param frames
  *   The sequence of frames to display
  * @param interval
  *   The interval in milliseconds between frames
  */
sealed trait SpinnerStyle:
  def frames: Seq[String]
  def interval: Int

object SpinnerStyle:
  /**
    * Braille dots spinner (default). Works well in most terminals.
    */
  case object Dots extends SpinnerStyle:
    val frames   = Seq("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")
    val interval = 80

  /**
    * Simple line spinner. ASCII-compatible.
    */
  case object Line extends SpinnerStyle:
    val frames   = Seq("-", "\\", "|", "/")
    val interval = 130

  /**
    * Arrow spinner.
    */
  case object Arrow extends SpinnerStyle:
    val frames   = Seq("←", "↖", "↑", "↗", "→", "↘", "↓", "↙")
    val interval = 120

  /**
    * Bouncing dots spinner.
    */
  case object Bounce extends SpinnerStyle:
    val frames   = Seq("⠁", "⠂", "⠄", "⡀", "⢀", "⠠", "⠐", "⠈")
    val interval = 120

  /**
    * Circle spinner.
    */
  case object Circle extends SpinnerStyle:
    val frames   = Seq("◐", "◓", "◑", "◒")
    val interval = 100

  /**
    * Square spinner.
    */
  case object Square extends SpinnerStyle:
    val frames   = Seq("◰", "◳", "◲", "◱")
    val interval = 100

  /**
    * Simple dots spinner. ASCII-compatible.
    */
  case object SimpleDots extends SpinnerStyle:
    val frames   = Seq(".", "..", "...")
    val interval = 300

  /**
    * Growing bar spinner.
    */
  case object GrowingBar extends SpinnerStyle:
    val frames   = Seq("▏", "▎", "▍", "▌", "▋", "▊", "▉", "█", "▉", "▊", "▋", "▌", "▍", "▎", "▏")
    val interval = 80

  /**
    * Moon phases spinner.
    */
  case object Moon extends SpinnerStyle:
    val frames   = Seq("🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘")
    val interval = 100

  /**
    * Clock spinner.
    */
  case object Clock extends SpinnerStyle:
    val frames   = Seq("🕐", "🕑", "🕒", "🕓", "🕔", "🕕", "🕖", "🕗", "🕘", "🕙", "🕚", "🕛")
    val interval = 100

  /**
    * Custom spinner with user-defined frames and interval.
    */
  case class Custom(frames: Seq[String], interval: Int) extends SpinnerStyle

  /**
    * Default spinner style.
    */
  val Default: SpinnerStyle = Dots

end SpinnerStyle
