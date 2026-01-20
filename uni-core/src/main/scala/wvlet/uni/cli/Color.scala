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
  * Color definitions for terminal text styling.
  *
  * Supports:
  *   - Standard 16 ANSI colors (8 normal + 8 bright)
  *   - 256-color palette (ANSI 256)
  *   - True color (24-bit RGB)
  */
enum Color:
  // Standard colors (ANSI 30-37 for foreground, 40-47 for background)
  case Black
  case Red
  case Green
  case Yellow
  case Blue
  case Magenta
  case Cyan
  case White

  // Bright colors (ANSI 90-97 for foreground, 100-107 for background)
  case BrightBlack // Gray
  case BrightRed
  case BrightGreen
  case BrightYellow
  case BrightBlue
  case BrightMagenta
  case BrightCyan
  case BrightWhite

  // Extended colors
  case Ansi256(code: Int)
  case Rgb(r: Int, g: Int, b: Int)

object Color:
  /**
    * Create a color from a hex string (e.g., "#FF5733" or "FF5733")
    */
  def fromHex(hex: String): Color =
    val cleaned = hex.stripPrefix("#")
    require(cleaned.length == 6, s"Invalid hex color: ${hex}")
    val r = Integer.parseInt(cleaned.substring(0, 2), 16)
    val g = Integer.parseInt(cleaned.substring(2, 4), 16)
    val b = Integer.parseInt(cleaned.substring(4, 6), 16)
    Color.Rgb(r, g, b)

  // ANSI escape code generators
  private[cli] def foregroundCode(color: Color): String =
    color match
      case Black =>
        "\u001b[30m"
      case Red =>
        "\u001b[31m"
      case Green =>
        "\u001b[32m"
      case Yellow =>
        "\u001b[33m"
      case Blue =>
        "\u001b[34m"
      case Magenta =>
        "\u001b[35m"
      case Cyan =>
        "\u001b[36m"
      case White =>
        "\u001b[37m"
      case BrightBlack =>
        "\u001b[90m"
      case BrightRed =>
        "\u001b[91m"
      case BrightGreen =>
        "\u001b[92m"
      case BrightYellow =>
        "\u001b[93m"
      case BrightBlue =>
        "\u001b[94m"
      case BrightMagenta =>
        "\u001b[95m"
      case BrightCyan =>
        "\u001b[96m"
      case BrightWhite =>
        "\u001b[97m"
      case Ansi256(code) =>
        s"\u001b[38;5;${code}m"
      case Rgb(r, g, b) =>
        s"\u001b[38;2;${r};${g};${b}m"

  private[cli] def backgroundCode(color: Color): String =
    color match
      case Black =>
        "\u001b[40m"
      case Red =>
        "\u001b[41m"
      case Green =>
        "\u001b[42m"
      case Yellow =>
        "\u001b[43m"
      case Blue =>
        "\u001b[44m"
      case Magenta =>
        "\u001b[45m"
      case Cyan =>
        "\u001b[46m"
      case White =>
        "\u001b[47m"
      case BrightBlack =>
        "\u001b[100m"
      case BrightRed =>
        "\u001b[101m"
      case BrightGreen =>
        "\u001b[102m"
      case BrightYellow =>
        "\u001b[103m"
      case BrightBlue =>
        "\u001b[104m"
      case BrightMagenta =>
        "\u001b[105m"
      case BrightCyan =>
        "\u001b[106m"
      case BrightWhite =>
        "\u001b[107m"
      case Ansi256(code) =>
        s"\u001b[48;5;${code}m"
      case Rgb(r, g, b) =>
        s"\u001b[48;2;${r};${g};${b}m"

  // Convenience alias
  val Gray: Color = BrightBlack

end Color
