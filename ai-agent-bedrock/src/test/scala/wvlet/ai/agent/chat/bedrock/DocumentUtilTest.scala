package wvlet.ai.agent.chat.bedrock

import software.amazon.awssdk.core.SdkNumber
import software.amazon.awssdk.core.document.Document
import wvlet.airspec.AirSpec

import scala.jdk.CollectionConverters.*

class DocumentUtilTest extends AirSpec:

  test("fromJson") {
    val json = """
    {
      "string": "hello",
      "int": 123,
      "long": 1234567890123,
      "double": 1.23,
      "boolean": true,
      "nullValue": null,
      "array": [1, "two", false],
      "map": {
        "nestedString": "world",
        "nestedInt": 456
      }
    }
    """

    val doc = DocumentUtil.fromJson(json)

    doc.isMap shouldBe true
    val map = doc.asMap().asScala

    map("string").asString() shouldBe "hello"
    map("int").asNumber().intValue() shouldBe 123
    map("long").asNumber().longValue() shouldBe 1234567890123L
    map("double").asNumber().doubleValue() shouldBe 1.23
    map("boolean").asBoolean() shouldBe true
    map("nullValue").isNull shouldBe true

    val arr = map("array").asList().asScala
    arr.size shouldBe 3
    arr(0).asNumber().intValue() shouldBe 1
    arr(1).asString() shouldBe "two"
    arr(2).asBoolean() shouldBe false

    val nestedMap = map("map").asMap().asScala
    nestedMap("nestedString").asString() shouldBe "world"
    nestedMap("nestedInt").asNumber().intValue() shouldBe 456
  }

  test("fromMap") {
    val data = Map(
      "string"     -> "hello",
      "int"        -> 123,
      "long"       -> 1234567890123L,
      "double"     -> 1.23,
      "boolean"    -> true,
      "nullValue"  -> null,
      "optionSome" -> Some("value"),
      "optionNone" -> None,
      "array"      -> Seq(1, "two", false),
      "map"        -> Map("nestedString" -> "world", "nestedInt" -> 456)
    )

    val doc = DocumentUtil.fromMap(data)

    doc.isMap shouldBe true
    val map = doc.asMap().asScala

    map("string").asString() shouldBe "hello"
    map("int").asNumber().intValue() shouldBe 123
    map("long").asNumber().longValue() shouldBe 1234567890123L
    map("double").asNumber().doubleValue() shouldBe 1.23
    map("boolean").asBoolean() shouldBe true
    map("nullValue").isNull shouldBe true
    map("optionSome").asString() shouldBe "value"
    map.contains("optionNone") shouldBe false // None should be omitted

    val arr = map("array").asList().asScala
    arr.size shouldBe 3
    arr(0).asNumber().intValue() shouldBe 1
    arr(1).asString() shouldBe "two"
    arr(2).asBoolean() shouldBe false

    val nestedMap = map("map").asMap().asScala
    nestedMap("nestedString").asString() shouldBe "world"
    nestedMap("nestedInt").asNumber().intValue() shouldBe 456
  }

  test("fromArray") {
    val data = Seq(
      "hello",
      123,
      1234567890123L,
      1.23,
      true,
      null,
      Seq(1, "two"),
      Map("key" -> "value")
    )

    val doc = DocumentUtil.fromArray(data)
    doc.isList shouldBe true
    val list = doc.asList().asScala

    list(0).asString() shouldBe "hello"
    list(1).asNumber().intValue() shouldBe 123
    list(2).asNumber().longValue() shouldBe 1234567890123L
    list(3).asNumber().doubleValue() shouldBe 1.23
    list(4).asBoolean() shouldBe true
    list(5).isNull shouldBe true

    val nestedList = list(6).asList().asScala
    nestedList(0).asNumber().intValue() shouldBe 1
    nestedList(1).asString() shouldBe "two"

    val nestedMap = list(7).asMap().asScala
    nestedMap("key").asString() shouldBe "value"
  }

  test("toMap") {
    val doc = Document
      .mapBuilder()
      .putString("string", "hello")
      .putNumber("int", 123)
      .putNumber("long", 1234567890123L)
      .putNumber("double", 1.23)
      .putBoolean("boolean", true)
      .putNull("nullValue")
      .putDocument(
        "array",
        Document.listBuilder().addNumber(1).addString("two").addBoolean(false).build()
      )
      .putDocument(
        "map",
        Document.mapBuilder().putString("nestedString", "world").putNumber("nestedInt", 456).build()
      )
      .build()

    val map = DocumentUtil.toMap(doc)

    map("string") shouldBe "hello"
    map("int") shouldBe SdkNumber.fromInteger(123) // Note: SdkNumber is preserved
    map("long") shouldBe SdkNumber.fromLong(1234567890123L)
    map("double") shouldBe SdkNumber.fromDouble(1.23)
    map("boolean") shouldBe true
    map("nullValue") shouldBe null

    val arr = map("array").asInstanceOf[List[Any]]
    arr(0) shouldBe SdkNumber.fromInteger(1)
    arr(1) shouldBe "two"
    arr(2) shouldBe false

    val nestedMap = map("map").asInstanceOf[Map[String, Any]]
    nestedMap("nestedString") shouldBe "world"
    nestedMap("nestedInt") shouldBe SdkNumber.fromInteger(456)
  }

end DocumentUtilTest
