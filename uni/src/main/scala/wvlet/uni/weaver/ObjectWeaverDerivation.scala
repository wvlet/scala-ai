package wvlet.uni.weaver

import scala.quoted.*
import wvlet.uni.surface.Surface
import wvlet.uni.weaver.codec.CaseClassWeaver

/**
  * Compile-time derivation of ObjectWeaver for case classes using Scala 3 macros.
  */
object ObjectWeaverDerivation:

  /**
    * Derive an ObjectWeaver for type A at compile-time.
    */
  inline def deriveWeaver[A]: ObjectWeaver[A] =
    ${
      deriveWeaverImpl[A]
    }

  private def deriveWeaverImpl[A: Type](using Quotes): Expr[ObjectWeaver[A]] =
    import quotes.reflect.*

    val tpe    = TypeRepr.of[A]
    val symbol = tpe.typeSymbol

    // Check if the type is a case class
    if !symbol.flags.is(Flags.Case) then
      report.errorAndAbort(
        s"ObjectWeaver.derived can only be used with case classes, but ${symbol
            .fullName} is not a case class"
      )

    // Get the surface for this type
    val surfaceExpr =
      '{
        Surface.of[A]
      }

    // Get the constructor parameters
    val params = symbol.primaryConstructor.paramSymss.flatten.filterNot(_.isTypeParam)

    // Build the field weavers expression for each parameter
    val fieldWeaverExprs: List[Expr[ObjectWeaver[?]]] = params.map { param =>
      val paramType = tpe.memberType(param)
      paramType.asType match
        case '[t] =>
          // Try to summon an ObjectWeaver for this type
          Expr.summon[ObjectWeaver[t]] match
            case Some(weaver) =>
              weaver.asExprOf[ObjectWeaver[?]]
            case None =>
              report.errorAndAbort(
                s"No ObjectWeaver found for field '${param.name}' of type ${paramType.show}. " +
                  s"Make sure an ObjectWeaver instance is available in scope."
              )
    }

    // Build IndexedSeq of field weavers
    val fieldWeaversExpr: Expr[IndexedSeq[ObjectWeaver[?]]] =
      '{
        IndexedSeq(
          ${
            Varargs(fieldWeaverExprs)
          }*
        )
      }

    // Create the CaseClassWeaver
    '{
      new CaseClassWeaver[A]($surfaceExpr, $fieldWeaversExpr)
    }

  end deriveWeaverImpl

end ObjectWeaverDerivation
