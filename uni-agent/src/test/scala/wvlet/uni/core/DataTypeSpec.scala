package wvlet.uni.core

import wvlet.uni.agent.core.DataType
import wvlet.unirspec.AirSpec

class DataTypeSpec extends AirSpec:

  test("primitive types should be distinct") {
    DataType.BooleanType shouldBe DataType.BooleanType
    DataType.StringType shouldBe DataType.StringType
    DataType.JsonType shouldBe DataType.JsonType
    DataType.IntegerType shouldBe DataType.IntegerType
    DataType.FloatType shouldBe DataType.FloatType
    DataType.ByteArrayType shouldBe DataType.ByteArrayType
    DataType.AnyType shouldBe DataType.AnyType
    DataType.ObjectType shouldBe DataType.ObjectType

    DataType.StringType shouldNotBe DataType.IntegerType
  }

  test("ArrayType should hold element type") {
    val arrType = DataType.ArrayType(DataType.StringType)
    arrType.elementType shouldBe DataType.StringType
    arrType shouldBe DataType.ArrayType(DataType.StringType)
    arrType shouldNotBe DataType.ArrayType(DataType.IntegerType)
  }

  test("RecordType should hold fields") {
    val field1     = DataType.Field("id", DataType.IntegerType, isRequired = true)
    val field2     = DataType.Field("name", DataType.StringType, isRequired = false)
    val recordType = DataType.RecordType(List(field1, field2))

    recordType.fields shouldBe List(field1, field2)
    recordType shouldBe DataType.RecordType(List(field1, field2))
    recordType shouldNotBe DataType.RecordType(List(field1))
  }

  test("OptionalType should hold data type") {
    val optType = DataType.OptionalType(DataType.FloatType)
    optType.dataType shouldBe DataType.FloatType
    optType shouldBe DataType.OptionalType(DataType.FloatType)
    optType shouldNotBe DataType.OptionalType(DataType.StringType)
  }

  test("GenericType should hold type parameters and fields") {
    val typeParam   = DataType.StringType
    val field1      = DataType.Field("key", typeParam, isRequired = true)
    val field2      = DataType.Field("value", DataType.AnyType, isRequired = true)
    val genericType = DataType.GenericType(List(typeParam), List(field1, field2))

    genericType.typeParams shouldBe List(typeParam)
    genericType.fields shouldBe List(field1, field2)
    genericType shouldBe DataType.GenericType(List(typeParam), List(field1, field2))
    genericType shouldNotBe DataType.GenericType(List(DataType.IntegerType), List(field1, field2))
  }

  test("Field should hold its properties") {
    val field = DataType.Field(
      name = "age",
      dataType = DataType.IntegerType,
      description = "User age",
      isRequired = true
    )
    field.name shouldBe "age"
    field.dataType shouldBe DataType.IntegerType
    field.description shouldBe "User age"
    field.isRequired shouldBe true

    val field2 = DataType.Field("age", DataType.IntegerType, "User age", true)
    field shouldBe field2

    val field3 = DataType.Field("name", DataType.StringType, "", false)
    field shouldNotBe field3
  }

end DataTypeSpec
