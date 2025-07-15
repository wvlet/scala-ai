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
package wvlet.ai.core.typeshape

import scala.language.existentials

/**
  */
import scala.language.existentials

/**
  * ObjectFactory is a trait for creating new instances of objects. It provides a generic way to
  * instantiate objects with given arguments.
  */
trait ObjectFactory extends Serializable:
  def newInstance(args: Seq[Any]): Any

object ObjectFactory:

  /**
    * Used internally for creating a new ObjectFactory instance from a given generic function
    * @param f
    * @return
    */
  def newFactory(f: Seq[Any] => Any): ObjectFactory =
    new ObjectFactory:
      override def newInstance(args: Seq[Any]): Any = f(args)

object Primitive:
  import java.lang as jl

  private[typeshape] val primitiveTable =
    val b = Map.newBuilder[Class[?], PrimitiveTypeShape]
    b += classOf[jl.Boolean]           -> Boolean
    b += classOf[Boolean]              -> Boolean
    b += classOf[jl.Short]             -> Short
    b += classOf[Short]                -> Short
    b += classOf[jl.Byte]              -> Byte
    b += classOf[Byte]                 -> Byte
    b += classOf[jl.Character]         -> Char
    b += classOf[Char]                 -> Char
    b += classOf[jl.Integer]           -> Int
    b += classOf[Int]                  -> Int
    b += classOf[jl.Float]             -> Float
    b += classOf[Float]                -> Float
    b += classOf[jl.Long]              -> Long
    b += classOf[Long]                 -> Long
    b += classOf[jl.Double]            -> Double
    b += classOf[Double]               -> Double
    b += classOf[jl.String]            -> String
    b += classOf[String]               -> String
    b += classOf[BigInt]               -> BigInt
    b += classOf[java.math.BigInteger] -> BigInteger
    b.result()

  def apply(cl: Class[?]): PrimitiveTypeShape = primitiveTable(cl)

  sealed abstract class PrimitiveTypeShape(override val rawType: Class[?])
      extends GenericTypeShape(rawType):
    override def isPrimitive: Boolean = true

  case object Int extends PrimitiveTypeShape(classOf[Int]):
    override def name: String     = "Int"
    override def fullName: String = "Int"

  case object Byte extends PrimitiveTypeShape(classOf[Byte]):
    override def name: String     = "Byte"
    override def fullName: String = "Byte"

  case object Long extends PrimitiveTypeShape(classOf[Long]):
    override def name: String     = "Long"
    override def fullName: String = "Long"

  case object Short extends PrimitiveTypeShape(classOf[Short]):
    override def name: String     = "Short"
    override def fullName: String = "Short"

  case object Boolean extends PrimitiveTypeShape(classOf[Boolean]):
    override def name: String     = "Boolean"
    override def fullName: String = "Boolean"

  case object Float extends PrimitiveTypeShape(classOf[Float]):
    override def name: String     = "Float"
    override def fullName: String = "Float"

  case object Double extends PrimitiveTypeShape(classOf[Double]):
    override def name: String     = "Double"
    override def fullName: String = "Double"

  case object Char extends PrimitiveTypeShape(classOf[Char]):
    override def name: String     = "Char"
    override def fullName: String = "Char"

  case object String extends PrimitiveTypeShape(classOf[String])

  case object Unit extends PrimitiveTypeShape(classOf[Unit]):
    override def name: String     = "Unit"
    override def fullName: String = "Unit"

  case object BigInt     extends PrimitiveTypeShape(classOf[BigInt])
  case object BigInteger extends PrimitiveTypeShape(classOf[java.math.BigInteger])

end Primitive

case class Alias(override val name: String, override val fullName: String, ref: TypeShape)
    extends GenericTypeShape(ref.rawType, ref.typeArgs, ref.params, ref.objectFactory):
  override def toString: String     = s"${name}:=${ref.name}"
  override def isAlias: Boolean     = true
  override def isPrimitive: Boolean = ref.isPrimitive
  override def isOption: Boolean    = ref.isOption
  override def dealias: TypeShape   = ref.dealias

case class HigherKindedTypeTypeShape(
    override val name: String,
    override val fullName: String,
    ref: TypeShape,
    override val typeArgs: Seq[TypeShape]
) extends GenericTypeShape(ref.rawType, typeArgs, ref.params, ref.objectFactory):
  override def toString: String =
    val s =
      if typeArgs.isEmpty then
        name
      else
        s"${name}[${typeArgs.mkString(",")}]"
    TypeName.sanitizeTypeName(s)

  override def isAlias: Boolean     = false
  override def isPrimitive: Boolean = ref.isPrimitive
  override def isOption: Boolean    = ref.isOption
  override def dealias: TypeShape   = ref.dealias

case object ExistentialType extends GenericTypeShape(classOf[Any]):
  override def name: String     = "?"
  override def fullName: String = "?"

case class ArrayTypeShape(override val rawType: Class[?], elementTypeShape: TypeShape)
    extends GenericTypeShape(rawType, Seq(elementTypeShape)):
  override def name: String     = s"Array[${elementTypeShape.name}]"
  override def fullName: String = s"Array[${elementTypeShape.fullName}]"
  override def toString: String = name

case class OptionTypeShape(override val rawType: Class[?], elementTypeShape: TypeShape)
    extends GenericTypeShape(rawType, Seq(elementTypeShape)):
  override def isOption: Boolean = true

case class JavaEnumTypeShape(override val rawType: Class[?]) extends GenericTypeShape(rawType)

/**
  * Enum-like surface for Scala 2.x and Scala 3
  * @param rawType
  * @param stringExtractor
  */
case class EnumTypeShape(
    override val rawType: Class[?],
    stringExtractor: (Class[?], String) => Option[Any]
) extends GenericTypeShape(rawType)

case class TupleTypeShape(override val rawType: Class[?], override val typeArgs: Seq[TypeShape])
    extends GenericTypeShape(rawType, typeArgs) {}

case class TaggedTypeShape(base: TypeShape, tag: TypeShape) extends TypeShape:
  override def toString: String         = name
  override def rawType: Class[?]        = base.rawType
  override def typeArgs: Seq[TypeShape] = base.typeArgs
  override def params: Seq[Parameter]   = base.params
  override def name: String             = s"${base.name}@@${tag.name}"
  override def fullName: String         = s"${base.fullName}@@${tag.fullName}"
  override def isOption: Boolean        = base.isOption
  override def isAlias: Boolean         = base.isAlias
  override def isPrimitive: Boolean     = base.isPrimitive
  override def dealias: TypeShape       = base.dealias

  override def objectFactory: Option[ObjectFactory] = base.objectFactory

/**
  * Represents Scala 3 intersection types, in which the left type is the primary type. This is
  * because applying intersection is challenging without knowing the application context.
  * @param left
  * @param right
  */
case class IntersectionTypeShape(left: TypeShape, right: TypeShape) extends TypeShape:
  override def toString: String                     = name
  override def rawType: Class[?]                    = left.rawType
  override def typeArgs: Seq[TypeShape]             = left.typeArgs
  override def params: Seq[Parameter]               = left.params
  override def name: String                         = s"${left.name}&${right.name}"
  override def fullName: String                     = s"${left.fullName}&${right.fullName}"
  override def isOption: Boolean                    = left.isOption
  override def isAlias: Boolean                     = left.isAlias
  override def isPrimitive: Boolean                 = left.isPrimitive
  override def dealias: TypeShape                   = left.dealias
  override def objectFactory: Option[ObjectFactory] = left.objectFactory

/**
  * Represents Scala 3 union types, in which the left type is the primary type. This is because
  * deciding which side of types in the union is difficult without knowing the application context.
  * @param left
  * @param right
  */
case class UnionTypeShape(left: TypeShape, right: TypeShape) extends TypeShape:
  override def toString: String                     = name
  override def rawType: Class[?]                    = left.rawType
  override def typeArgs: Seq[TypeShape]             = left.typeArgs
  override def params: Seq[Parameter]               = left.params
  override def name: String                         = s"${left.name}|${right.name}"
  override def fullName: String                     = s"${left.fullName}|${right.fullName}"
  override def isOption: Boolean                    = left.isOption
  override def isAlias: Boolean                     = left.isAlias
  override def isPrimitive: Boolean                 = left.isPrimitive
  override def dealias: TypeShape                   = UnionTypeShape(left.dealias, right.dealias)
  override def objectFactory: Option[ObjectFactory] = left.objectFactory.orElse(right.objectFactory)

case object AnyRefTypeShape extends GenericTypeShape(classOf[AnyRef]):
  override def name: String = "AnyRef"

/**
  * Base class for generic surfaces with type args
  *
  * @param rawType
  * @param typeArgs
  * @param params
  * @param objectFactory
  */
class GenericTypeShape(
    override val rawType: Class[?],
    override val typeArgs: Seq[TypeShape] = Seq.empty,
    override val params: Seq[Parameter] = Seq.empty,
    override val objectFactory: Option[ObjectFactory] = None
) extends TypeShape:
  private def getClassName: String =
    try
      TypeName.sanitizeTypeName(rawType.getSimpleName)
    catch
      case e: InternalError =>
        // Scala REPL use class name like $line3.$read$$iw$$iw$A, which causes InternalError at getSimpleName
        TypeName.sanitizeTypeName(rawType.getName)

  override def name: String =
    val clsName = TypeName.sanitizeTypeName(getClassName)
    val s =
      if typeArgs.isEmpty then
        clsName
      else
        s"${clsName}[${typeArgs.map(_.name).mkString(",")}]"
    TypeName.sanitizeTypeName(s)

  override def fullName: String =
    val clsName = TypeName.sanitizeTypeName(rawType.getName)
    val s =
      if typeArgs.isEmpty then
        clsName
      else
        s"${clsName}[${typeArgs.map(_.fullName).mkString(",")}]"
    TypeName.sanitizeTypeName(s)

  override def isOption: Boolean    = false
  override def isAlias: Boolean     = false
  override def isPrimitive: Boolean = false

  override def toString: String = name

  override def equals(obj: Any): Boolean =
    obj match
      case f: TypeShape =>
        this.fullName.equals(f.fullName)
      case _ =>
        false

  override def hashCode(): Int = fullName.hashCode

end GenericTypeShape

/**
  * TypeShape placeholder for supporting recursive types
  * @param rawType
  */
case class LazyTypeShape(override val rawType: Class[?], fullName: String) extends TypeShape:
  // Resolved the final type from the full TypeShape name
  protected def ref: TypeShape = TypeShape.getCached(fullName)

  def name: String =
    val s =
      if typeArgs.isEmpty then
        rawType.getSimpleName
      else
        s"${rawType.getSimpleName}[${typeArgs.map(_.name).mkString(",")}]"
    TypeName.sanitizeTypeName(s)

  override def toString: String                     = name
  override def params                               = ref.params
  override def typeArgs: Seq[TypeShape]             = ref.typeArgs
  override def isOption                             = ref.isOption
  override def isAlias                              = ref.isAlias
  override def isPrimitive                          = ref.isPrimitive
  override def objectFactory: Option[ObjectFactory] = ref.objectFactory

case class ClassMethodTypeShape(
    mod: Int,
    owner: TypeShape,
    name: String,
    returnType: TypeShape,
    args: Seq[MethodParameter],
    methodCaller: Option[(Any, Seq[Any]) => Any]
) extends MethodTypeShape:
  override def call(obj: Any, x: Any*) =
    def unsupported =
      throw new UnsupportedOperationException(s"Calling method ${name} is not supported: ${this}")

    methodCaller
      .map { caller =>
        caller(obj, x.toSeq)
      }
      .getOrElse {
        unsupported
      }
