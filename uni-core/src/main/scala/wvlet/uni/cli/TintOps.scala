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
  * Extension methods for String to apply Tint styling.
  *
  * Usage:
  * {{{
  * import wvlet.uni.cli.TintOps.*
  *
  * // Single style application
  * "Error!".red
  * "Success".green
  * "Warning".yellow
  *
  * // For multiple styles, use Tint directly or the tint method:
  * "Error!".tint(Tint.red.bold)
  * Tint.red.bold("Error!")
  * }}}
  *
  * Note: Chaining extension methods (e.g., "text".red.bold) is not recommended as it creates nested
  * ANSI escape codes. Use `Tint.red.bold("text")` or `"text".tint(Tint.red.bold)` instead.
  */
object TintOps:
  extension (s: String)
    // Foreground colors
    def black: String   = Tint.black(s)
    def red: String     = Tint.red(s)
    def green: String   = Tint.green(s)
    def yellow: String  = Tint.yellow(s)
    def blue: String    = Tint.blue(s)
    def magenta: String = Tint.magenta(s)
    def cyan: String    = Tint.cyan(s)
    def white: String   = Tint.white(s)
    def gray: String    = Tint.gray(s)

    // Bright foreground colors
    def brightBlack: String   = Tint.brightBlack(s)
    def brightRed: String     = Tint.brightRed(s)
    def brightGreen: String   = Tint.brightGreen(s)
    def brightYellow: String  = Tint.brightYellow(s)
    def brightBlue: String    = Tint.brightBlue(s)
    def brightMagenta: String = Tint.brightMagenta(s)
    def brightCyan: String    = Tint.brightCyan(s)
    def brightWhite: String   = Tint.brightWhite(s)

    // Text modifiers
    def bold: String          = Tint.bold(s)
    def dim: String           = Tint.dim(s)
    def italic: String        = Tint.italic(s)
    def underline: String     = Tint.underline(s)
    def strikethrough: String = Tint.strikethrough(s)
    def inverse: String       = Tint.inverse(s)
    def hidden: String        = Tint.hidden(s)

    /**
      * Apply a custom Tint style to this string.
      */
    def tint(style: Tint): String = style(s)

  end extension

end TintOps
