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
package wvlet.ai.core.util

/**
  * A helper to find the source code location, it is used for tracking the log location, design
  * configuration location, etc.
  */
case class SourceCode(fileName: String, line: Int, col: Int):
  override def toString = fileLocation
  def fileLocation      = s"${fileName}:${line}"

object SourceCode:
  def apply()(using code: SourceCode) = code

  import scala.quoted.*

  inline implicit def generate: SourceCode = ${ generateImpl }

  private def generateImpl(using q: Quotes): Expr[SourceCode] =
    import q.reflect.*
    val pos    = Position.ofMacroExpansion
    val line   = Expr(pos.startLine)
    val column = Expr(pos.endColumn)
    val src    = pos.sourceFile
    // Hidden because embedding the absolute path is not good for privacy and code size
    val srcPath: java.nio.file.Path = java.nio.file.Paths.get(src.path)
    val fileName                    = Expr(srcPath.getFileName().toString)
    '{ SourceCode(${ fileName }, ${ line } + 1, ${ column }) }

end SourceCode
