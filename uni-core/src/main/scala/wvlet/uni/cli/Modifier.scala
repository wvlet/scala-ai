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
  * Text modifiers for terminal styling.
  */
enum Modifier:
  case Bold
  case Dim
  case Italic
  case Underline
  case Strikethrough
  case Inverse
  case Hidden

object Modifier:
  private[cli] def code(modifier: Modifier): String =
    modifier match
      case Bold =>
        "\u001b[1m"
      case Dim =>
        "\u001b[2m"
      case Italic =>
        "\u001b[3m"
      case Underline =>
        "\u001b[4m"
      case Strikethrough =>
        "\u001b[9m"
      case Inverse =>
        "\u001b[7m"
      case Hidden =>
        "\u001b[8m"
