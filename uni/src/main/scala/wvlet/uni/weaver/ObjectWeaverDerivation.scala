package wvlet.uni.weaver

import scala.quoted.*
import wvlet.uni.surface.Surface
import wvlet.uni.weaver.codec.CaseClassWeaver
import wvlet.uni.weaver.codec.SealedTraitWeaver

/**
  * Compile-time derivation of ObjectWeaver for case classes and sealed traits using Scala 3 macros.
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
    val flags  = symbol.flags

    // Check if sealed trait or sealed abstract class
    if flags.is(Flags.Sealed) && (flags.is(Flags.Trait) || flags.is(Flags.Abstract)) then
      deriveSealedTraitWeaver[A]
    else if flags.is(Flags.Case) then
      deriveCaseClassWeaver[A]
    else
      report.errorAndAbort(
        s"ObjectWeaver.derived can only be used with case classes or sealed traits, but ${symbol
            .fullName} is neither"
      )

  end deriveWeaverImpl

  private def deriveCaseClassWeaver[A: Type](using Quotes): Expr[ObjectWeaver[A]] =
    import quotes.reflect.*

    val tpe    = TypeRepr.of[A]
    val symbol = tpe.typeSymbol

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

  end deriveCaseClassWeaver

  private def deriveSealedTraitWeaver[A: Type](using Quotes): Expr[ObjectWeaver[A]] =
    import quotes.reflect.*

    val tpe       = TypeRepr.of[A]
    val symbol    = tpe.typeSymbol
    val traitName = symbol.name

    // Get all direct children of the sealed trait
    val children = symbol.children

    if children.isEmpty then
      report.errorAndAbort(
        s"Sealed trait ${symbol
            .fullName} has no children. Add case classes or case objects that extend it."
      )

    // Build child weaver entries: (name, (weaver, Option[singleton]))
    val childEntries: List[Expr[(String, (ObjectWeaver[? <: A], Option[A]))]] = children.map {
      childSym =>
        val childName    = childSym.name.stripSuffix("$")
        val isCaseObject = childSym.flags.is(Flags.Module)
        val childType    =
          if isCaseObject then
            childSym.termRef
          else
            childSym.typeRef

        childType.asType match
          case '[t] =>
            Expr.summon[ObjectWeaver[t]] match
              case Some(weaverExpr) =>
                val singletonExpr: Expr[Option[A]] =
                  if isCaseObject then
                    val moduleRef = Ref(childSym)
                    '{
                      Some(
                        ${
                          moduleRef.asExprOf[t]
                        }.asInstanceOf[A]
                      )
                    }
                  else
                    '{
                      None
                    }

                '{
                  (
                    ${
                      Expr(childName)
                    },
                    (
                      ${
                        weaverExpr
                      }.asInstanceOf[ObjectWeaver[? <: A]],
                      $singletonExpr
                    )
                  )
                }
              case None =>
                val targetType =
                  if isCaseObject then
                    "case object"
                  else
                    "child type"
                report.errorAndAbort(
                  s"No ObjectWeaver found for ${targetType} '${childName}' of sealed trait ${symbol
                      .fullName}. " + s"Make sure it has 'derives ObjectWeaver'."
                )
        end match
    }

    // Build the Map expression
    val mapExpr: Expr[Map[String, (ObjectWeaver[? <: A], Option[A])]] =
      '{
        Map(
          ${
            Varargs(childEntries)
          }*
        )
      }

    // Create the SealedTraitWeaver
    '{
      new SealedTraitWeaver[A](
        ${
          Expr(traitName)
        },
        $mapExpr
      )
    }

  end deriveSealedTraitWeaver

end ObjectWeaverDerivation
