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
package wvlet.uni.cli.launcher

import scala.collection.mutable

/**
  * Tokenizes command line strings into Array[String]. Handles quoted strings and escape sequences.
  */
object CommandLineTokenizer:
  /**
    * Tokenize a command line string into an array of arguments. Handles:
    *   - Single quotes: 'hello world' → hello world
    *   - Double quotes: "hello world" → hello world
    *   - Escaped characters: hello\ world → hello world
    *   - Mixed: 'hello "world"' → hello "world"
    */
  def tokenize(line: String): Array[String] =
    if line == null || line.trim.isEmpty then
      return Array.empty

    val tokens        = mutable.Buffer[String]()
    val current       = StringBuilder()
    var i             = 0
    var inSingleQuote = false
    var inDoubleQuote = false

    while i < line.length do
      val c = line.charAt(i)

      if inSingleQuote then
        if c == '\'' then
          inSingleQuote = false
        else
          current.append(c)
      else if inDoubleQuote then
        if c == '"' then
          inDoubleQuote = false
        else if c == '\\' && i + 1 < line.length then
          val next = line.charAt(i + 1)
          next match
            case '"' | '\\' =>
              current.append(next)
              i += 1
            case 'n' =>
              current.append('\n')
              i += 1
            case 't' =>
              current.append('\t')
              i += 1
            case 'r' =>
              current.append('\r')
              i += 1
            case _ =>
              current.append(c)
        else
          current.append(c)
      else
        c match
          case '\'' =>
            inSingleQuote = true
          case '"' =>
            inDoubleQuote = true
          case '\\' if i + 1 < line.length =>
            // Escape next character
            current.append(line.charAt(i + 1))
            i += 1
          case ' ' | '\t' | '\n' | '\r' =>
            // Whitespace - end current token if non-empty
            if current.nonEmpty then
              tokens += current.toString
              current.clear()
          case _ =>
            current.append(c)
      end if

      i += 1
    end while

    // Add final token if non-empty
    if current.nonEmpty then
      tokens += current.toString

    // Check for unclosed quotes
    if inSingleQuote then
      throw IllegalArgumentException("Unclosed single quote")
    if inDoubleQuote then
      throw IllegalArgumentException("Unclosed double quote")

    tokens.toArray

  end tokenize

end CommandLineTokenizer
