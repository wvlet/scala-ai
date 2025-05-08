package wvlet.ai.util.log

object LoggerMacros:
  import scala.quoted.*

  inline def sourcePos(): LogSource =
    ${
      sourcePos
    }

  private def sourcePos(using q: Quotes): Expr[wvlet.ai.util.log.LogSource] =
    import q.reflect.*
    val pos                         = Position.ofMacroExpansion
    val line                        = Expr(pos.startLine)
    val column                      = Expr(pos.endColumn)
    val src                         = pos.sourceFile
    val srcPath: java.nio.file.Path = java.nio.file.Paths.get(src.path)
    val fileName                    = Expr(srcPath.getFileName().toString)
    '{
      LogSource(
        ${
          fileName
        },
        ${
          line
        } + 1,
        ${
          column
        }
      )
    }

end LoggerMacros
