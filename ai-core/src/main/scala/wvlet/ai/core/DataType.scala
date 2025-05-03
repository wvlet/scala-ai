package wvlet.ai.core

import wvlet.ai.core.DataType.OptionalType

trait DataType:
  def isRequired: Boolean =
    this match
      case o: OptionalType =>
        false
      case _ =>
        true

object DataType:
  case object BooleanType                                                 extends DataType
  case object StringType                                                  extends DataType
  case object JsonType                                                    extends DataType
  case object IntegerType                                                 extends DataType
  case object FloatType                                                   extends DataType
  case object ByteArrayType                                               extends DataType
  case object AnyType                                                     extends DataType
  case object ObjectType                                                  extends DataType
  case class ArrayType(elementType: DataType)                             extends DataType
  case class RecordType(fields: List[Field])                              extends DataType
  case class OptionalType(dataType: DataType)                             extends DataType
  case class GenericType(typeParams: List[DataType], fields: List[Field]) extends DataType

  case class Field(name: String, dataType: DataType, description: String = "", isRequired: Boolean)
