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
  * Terminal utilities for cursor control and screen manipulation.
  */
object Terminal:
  // ANSI escape sequences
  private val ESC = "\u001b"
  private val CSI = s"${ESC}["

  /**
    * Returns the terminal width in columns.
    */
  def width: Int = CliEnv.terminalWidth

  /**
    * Returns the terminal height in rows.
    */
  def height: Int = CliEnv.terminalHeight

  /**
    * Returns true if the output is connected to an interactive terminal.
    */
  def isInteractive: Boolean = CliEnv.isInteractive

  /**
    * Returns true if the terminal supports color output.
    */
  def supportsColor: Boolean = CliEnv.supportsColor

  /**
    * Returns the color support level of the terminal.
    */
  def colorLevel: ColorLevel = CliEnv.colorLevel

  /**
    * Clear the entire screen.
    */
  def clear(out: PrintStream = System.err): Unit =
    out.print(s"${CSI}2J${CSI}H")
    out.flush()

  /**
    * Clear from cursor to end of screen.
    */
  def clearToEnd(out: PrintStream = System.err): Unit =
    out.print(s"${CSI}0J")
    out.flush()

  /**
    * Clear from cursor to beginning of screen.
    */
  def clearToBeginning(out: PrintStream = System.err): Unit =
    out.print(s"${CSI}1J")
    out.flush()

  /**
    * Clear the current line.
    */
  def clearLine(out: PrintStream = System.err): Unit =
    out.print(s"${CSI}2K")
    out.flush()

  /**
    * Clear from cursor to end of line.
    */
  def clearLineToEnd(out: PrintStream = System.err): Unit =
    out.print(s"${CSI}0K")
    out.flush()

  /**
    * Clear from cursor to beginning of line.
    */
  def clearLineToBeginning(out: PrintStream = System.err): Unit =
    out.print(s"${CSI}1K")
    out.flush()

  /**
    * Move cursor to the specified position (1-based).
    */
  def moveCursor(row: Int, col: Int, out: PrintStream = System.err): Unit =
    out.print(s"${CSI}${row};${col}H")
    out.flush()

  /**
    * Move cursor up by the specified number of lines.
    */
  def moveCursorUp(lines: Int = 1, out: PrintStream = System.err): Unit =
    out.print(s"${CSI}${lines}A")
    out.flush()

  /**
    * Move cursor down by the specified number of lines.
    */
  def moveCursorDown(lines: Int = 1, out: PrintStream = System.err): Unit =
    out.print(s"${CSI}${lines}B")
    out.flush()

  /**
    * Move cursor right by the specified number of columns.
    */
  def moveCursorRight(cols: Int = 1, out: PrintStream = System.err): Unit =
    out.print(s"${CSI}${cols}C")
    out.flush()

  /**
    * Move cursor left by the specified number of columns.
    */
  def moveCursorLeft(cols: Int = 1, out: PrintStream = System.err): Unit =
    out.print(s"${CSI}${cols}D")
    out.flush()

  /**
    * Move cursor to the beginning of the line.
    */
  def moveCursorToLineStart(out: PrintStream = System.err): Unit =
    out.print("\r")
    out.flush()

  /**
    * Hide the cursor.
    */
  def hideCursor(out: PrintStream = System.err): Unit =
    out.print(s"${CSI}?25l")
    out.flush()

  /**
    * Show the cursor.
    */
  def showCursor(out: PrintStream = System.err): Unit =
    out.print(s"${CSI}?25h")
    out.flush()

  /**
    * Save the current cursor position.
    */
  def saveCursor(out: PrintStream = System.err): Unit =
    out.print(s"${CSI}s")
    out.flush()

  /**
    * Restore the previously saved cursor position.
    */
  def restoreCursor(out: PrintStream = System.err): Unit =
    out.print(s"${CSI}u")
    out.flush()

end Terminal
