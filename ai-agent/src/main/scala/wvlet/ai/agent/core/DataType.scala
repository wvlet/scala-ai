package wvlet.ai.agent.core

import DataType.OptionalType

import scala.Predef.???

trait DataType:
  def jsonType: String
  def isRequired: Boolean =
    this match
      case o: OptionalType =>
        false
      case _ =>
        true

object DataType:
  case object NullType extends DataType:
    override def jsonType: String = "null"

  case object BooleanType extends DataType:
    override def jsonType: String = "boolean"

  case object StringType extends DataType:
    override def jsonType: String = "string"

  case object JsonType extends DataType:
    override def jsonType: String = "json"

  case object IntegerType extends DataType:
    override def jsonType: String = "integer"

  case object FloatType extends DataType:
    override def jsonType: String = "number"

  case object ByteArrayType extends DataType:
    override def jsonType: String = "string"

  case object AnyType extends DataType:
    override def jsonType: String = "object"

  case object ObjectType extends DataType:
    override def jsonType: String = "object"

  case class ArrayType(elementType: DataType) extends DataType:
    override def jsonType: String = "array"

  case class RecordType(fields: List[Field]) extends DataType:
    override def jsonType: String = "object"

  case class OptionalType(dataType: DataType) extends DataType:
    override def jsonType: String = dataType.jsonType

  case class GenericType(typeParams: List[DataType], fields: List[Field]) extends DataType:
    override def jsonType: String = "object"

  case class Field(name: String, dataType: DataType, description: String = "", isRequired: Boolean)

end DataType
