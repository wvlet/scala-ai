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
package wvlet.uni.test

/**
  * A helper to find the source code location for test assertions. Captures both the location and
  * the actual source line at compile time for better error reporting.
  */
case class TestSource(fileName: String, line: Int, col: Int, sourceLine: String = ""):
  def fileLocation      = s"${fileName}:${line}"
  override def toString = fileLocation

  /**
    * Format the source location as a code snippet with line number and column marker
    */
  def formatSnippet: Option[String] =
    if sourceLine.isEmpty then
      None
    else
      val linePrefix = s"${line}| "
      val snippet    = s"${linePrefix}${sourceLine}"
      if col > 0 then
        val caretLine = " " * (linePrefix.length + col - 1) + "^"
        Some(s"${snippet}\n${caretLine}")
      else
        Some(snippet)

object TestSource:
  def apply()(using code: TestSource) = code

  import scala.quoted.*

  inline implicit def generate: TestSource =
    ${
      generateImpl
    }

  private def generateImpl(using q: Quotes): Expr[TestSource] =
    import q.reflect.*
    val pos    = Position.ofMacroExpansion
    val line   = Expr(pos.startLine)
    val column = Expr(pos.endColumn)
    val src    = pos.sourceFile
    // Hidden because embedding the absolute path is not good for privacy and code size
    val srcPath: java.nio.file.Path = java.nio.file.Paths.get(src.path)
    val fileName                    = Expr(srcPath.getFileName().toString)

    // Capture the source line content at compile time
    val sourceLineContent = src
      .content
      .flatMap { content =>
        val lines = content.linesIterator.toIndexedSeq
        if pos.startLine >= 0 && pos.startLine < lines.size then
          Some(lines(pos.startLine).trim)
        else
          None
      }
    val sourceLine = Expr(sourceLineContent.getOrElse(""))

    '{
      TestSource(
        ${
          fileName
        },
        ${
          line
        } + 1,
        ${
          column
        },
        ${
          sourceLine
        }
      )
    }

  end generateImpl

end TestSource
