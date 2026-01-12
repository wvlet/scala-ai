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
  * Extension methods for String to apply Chalk styling.
  *
  * Usage:
  * {{{
  * import wvlet.uni.cli.ChalkOps.*
  *
  * "Error!".red.bold
  * "Success".green
  * "Warning".yellow.underline
  * }}}
  */
object ChalkOps:
  extension (s: String)
    // Foreground colors
    def black: String   = Chalk.black(s)
    def red: String     = Chalk.red(s)
    def green: String   = Chalk.green(s)
    def yellow: String  = Chalk.yellow(s)
    def blue: String    = Chalk.blue(s)
    def magenta: String = Chalk.magenta(s)
    def cyan: String    = Chalk.cyan(s)
    def white: String   = Chalk.white(s)
    def gray: String    = Chalk.gray(s)

    // Bright foreground colors
    def brightBlack: String   = Chalk.brightBlack(s)
    def brightRed: String     = Chalk.brightRed(s)
    def brightGreen: String   = Chalk.brightGreen(s)
    def brightYellow: String  = Chalk.brightYellow(s)
    def brightBlue: String    = Chalk.brightBlue(s)
    def brightMagenta: String = Chalk.brightMagenta(s)
    def brightCyan: String    = Chalk.brightCyan(s)
    def brightWhite: String   = Chalk.brightWhite(s)

    // Text modifiers
    def bold: String          = Chalk.bold(s)
    def dim: String           = Chalk.dim(s)
    def italic: String        = Chalk.italic(s)
    def underline: String     = Chalk.underline(s)
    def strikethrough: String = Chalk.strikethrough(s)
    def inverse: String       = Chalk.inverse(s)
    def hidden: String        = Chalk.hidden(s)

    /**
      * Apply a custom Chalk style to this string.
      */
    def chalk(style: Chalk): String = style(s)

  end extension

end ChalkOps
