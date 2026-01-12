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
  * Chalk provides fluent API for terminal text styling.
  *
  * Usage:
  * {{{
  * import wvlet.uni.cli.Chalk
  *
  * // Method chaining
  * Chalk.red.bold("Error!")
  * Chalk.green("Success")
  * Chalk.yellow.bgBlue.underline("Warning")
  *
  * // Reusable styles
  * val errorStyle = Chalk.red.bold
  * val successStyle = Chalk.green
  * println(errorStyle("Failed!"))
  * println(successStyle("Passed!"))
  *
  * // Extended colors
  * Chalk.hex("#FF5733")("Custom color")
  * Chalk.rgb(255, 87, 51)("RGB color")
  * Chalk.ansi256(208)("256-color")
  * }}}
  */
case class Chalk(
    foreground: Option[Color] = None,
    background: Option[Color] = None,
    modifiers: Set[Modifier] = Set.empty,
    enabled: Boolean = true
):
  private val RESET = "\u001b[0m"

  /**
    * Apply the style to the given text.
    */
  def apply(text: String): String =
    if !enabled || (foreground.isEmpty && background.isEmpty && modifiers.isEmpty) then
      text
    else
      val sb = StringBuilder()
      foreground.foreach(c => sb.append(Color.foregroundCode(c)))
      background.foreach(c => sb.append(Color.backgroundCode(c)))
      modifiers.foreach(m => sb.append(Modifier.code(m)))
      sb.append(text)
      sb.append(RESET)
      sb.toString

  // Foreground colors
  def black: Chalk   = withForeground(Color.Black)
  def red: Chalk     = withForeground(Color.Red)
  def green: Chalk   = withForeground(Color.Green)
  def yellow: Chalk  = withForeground(Color.Yellow)
  def blue: Chalk    = withForeground(Color.Blue)
  def magenta: Chalk = withForeground(Color.Magenta)
  def cyan: Chalk    = withForeground(Color.Cyan)
  def white: Chalk   = withForeground(Color.White)
  def gray: Chalk    = withForeground(Color.Gray)

  // Bright foreground colors
  def brightBlack: Chalk   = withForeground(Color.BrightBlack)
  def brightRed: Chalk     = withForeground(Color.BrightRed)
  def brightGreen: Chalk   = withForeground(Color.BrightGreen)
  def brightYellow: Chalk  = withForeground(Color.BrightYellow)
  def brightBlue: Chalk    = withForeground(Color.BrightBlue)
  def brightMagenta: Chalk = withForeground(Color.BrightMagenta)
  def brightCyan: Chalk    = withForeground(Color.BrightCyan)
  def brightWhite: Chalk   = withForeground(Color.BrightWhite)

  // Background colors
  def bgBlack: Chalk   = withBackground(Color.Black)
  def bgRed: Chalk     = withBackground(Color.Red)
  def bgGreen: Chalk   = withBackground(Color.Green)
  def bgYellow: Chalk  = withBackground(Color.Yellow)
  def bgBlue: Chalk    = withBackground(Color.Blue)
  def bgMagenta: Chalk = withBackground(Color.Magenta)
  def bgCyan: Chalk    = withBackground(Color.Cyan)
  def bgWhite: Chalk   = withBackground(Color.White)
  def bgGray: Chalk    = withBackground(Color.Gray)

  // Bright background colors
  def bgBrightBlack: Chalk   = withBackground(Color.BrightBlack)
  def bgBrightRed: Chalk     = withBackground(Color.BrightRed)
  def bgBrightGreen: Chalk   = withBackground(Color.BrightGreen)
  def bgBrightYellow: Chalk  = withBackground(Color.BrightYellow)
  def bgBrightBlue: Chalk    = withBackground(Color.BrightBlue)
  def bgBrightMagenta: Chalk = withBackground(Color.BrightMagenta)
  def bgBrightCyan: Chalk    = withBackground(Color.BrightCyan)
  def bgBrightWhite: Chalk   = withBackground(Color.BrightWhite)

  // Text modifiers
  def bold: Chalk          = withModifier(Modifier.Bold)
  def dim: Chalk           = withModifier(Modifier.Dim)
  def italic: Chalk        = withModifier(Modifier.Italic)
  def underline: Chalk     = withModifier(Modifier.Underline)
  def strikethrough: Chalk = withModifier(Modifier.Strikethrough)
  def inverse: Chalk       = withModifier(Modifier.Inverse)
  def hidden: Chalk        = withModifier(Modifier.Hidden)

  // Extended colors - foreground
  def rgb(r: Int, g: Int, b: Int): Chalk = withForeground(Color.Rgb(r, g, b))
  def hex(hexColor: String): Chalk       = withForeground(Color.fromHex(hexColor))
  def ansi256(code: Int): Chalk          = withForeground(Color.Ansi256(code))

  // Extended colors - background
  def bgRgb(r: Int, g: Int, b: Int): Chalk = withBackground(Color.Rgb(r, g, b))
  def bgHex(hexColor: String): Chalk       = withBackground(Color.fromHex(hexColor))
  def bgAnsi256(code: Int): Chalk          = withBackground(Color.Ansi256(code))

  // Builder methods
  def withForeground(color: Color): Chalk     = this.copy(foreground = Some(color))
  def withBackground(color: Color): Chalk     = this.copy(background = Some(color))
  def withModifier(modifier: Modifier): Chalk = this.copy(modifiers = modifiers + modifier)
  def withEnabled(enabled: Boolean): Chalk    = this.copy(enabled = enabled)

  /**
    * Disable color output. Returns plain text without ANSI codes.
    */
  def noColor: Chalk = this.copy(enabled = false)

  /**
    * Reset this chalk instance to default (no styling).
    */
  def reset: Chalk = Chalk()

end Chalk

/**
  * Companion object providing static shortcuts for common colors.
  */
object Chalk:
  private val default = Chalk(None, None, Set.empty, true)

  /**
    * Create a new Chalk instance with default settings.
    */
  def apply(): Chalk = default

  // Fluent API methods that return a styled Chalk instance
  // Use as Chalk.red("text") which calls Chalk.red.apply("text")
  def black: Chalk   = default.black
  def red: Chalk     = default.red
  def green: Chalk   = default.green
  def yellow: Chalk  = default.yellow
  def blue: Chalk    = default.blue
  def magenta: Chalk = default.magenta
  def cyan: Chalk    = default.cyan
  def white: Chalk   = default.white
  def gray: Chalk    = default.gray

  def brightBlack: Chalk   = default.brightBlack
  def brightRed: Chalk     = default.brightRed
  def brightGreen: Chalk   = default.brightGreen
  def brightYellow: Chalk  = default.brightYellow
  def brightBlue: Chalk    = default.brightBlue
  def brightMagenta: Chalk = default.brightMagenta
  def brightCyan: Chalk    = default.brightCyan
  def brightWhite: Chalk   = default.brightWhite

  def bold: Chalk          = default.bold
  def dim: Chalk           = default.dim
  def italic: Chalk        = default.italic
  def underline: Chalk     = default.underline
  def strikethrough: Chalk = default.strikethrough
  def inverse: Chalk       = default.inverse
  def hidden: Chalk        = default.hidden

  // Extended colors
  def rgb(r: Int, g: Int, b: Int): Chalk = default.rgb(r, g, b)
  def hex(hexColor: String): Chalk       = default.hex(hexColor)
  def ansi256(code: Int): Chalk          = default.ansi256(code)

  // Background colors
  def bgBlack: Chalk   = default.bgBlack
  def bgRed: Chalk     = default.bgRed
  def bgGreen: Chalk   = default.bgGreen
  def bgYellow: Chalk  = default.bgYellow
  def bgBlue: Chalk    = default.bgBlue
  def bgMagenta: Chalk = default.bgMagenta
  def bgCyan: Chalk    = default.bgCyan
  def bgWhite: Chalk   = default.bgWhite
  def bgGray: Chalk    = default.bgGray

end Chalk
