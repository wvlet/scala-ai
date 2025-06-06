package wvlet.ai.core.weaver.codec

import wvlet.ai.core.msgpack.spi.MessagePack
import wvlet.ai.core.weaver.ObjectWeaver
import wvlet.airspec.AirSpec

class StringWeaverTest extends AirSpec:

  test("pack/unpack String") {
    // Test roundtrip String serialization
    val testStrings = Seq(
      "",
      "hello",
      "1234",
      "特殊文字",
      "a" * 1000 // long string
    )

    for str <- testStrings do
      val packed   = ObjectWeaver.weave(str)
      val unpacked = ObjectWeaver.unweave[String](packed)
      unpacked shouldBe str
  }

  test("unpack String from INTEGER types") {
    // Test integer to String conversion
    val testCases = Seq(
      (0, "0"),
      (1, "1"),
      (-1, "-1"),
      (Int.MaxValue, Int.MaxValue.toString),
      (Int.MinValue, Int.MinValue.toString)
    )

    for (intValue, expectedStr) <- testCases do
      val packer = MessagePack.newPacker()
      packer.packInt(intValue)
      val packed   = packer.toByteArray
      val unpacked = ObjectWeaver.unweave[String](packed)
      unpacked shouldBe expectedStr
  }

  test("unpack String from FLOAT types") {
    // Test float to String conversion
    // Note: String representation of doubles may differ between JVM and JS
    // so we check for semantic equivalence instead of exact string matching
    val testCases = Seq((0.0, "0"), (1.0, "1"), (-1.0, "-1"), (3.14, "3.14"))

    for (floatValue, expectedStr) <- testCases do
      val packer = MessagePack.newPacker()
      packer.packDouble(floatValue)
      val packed   = packer.toByteArray
      val unpacked = ObjectWeaver.unweave[String](packed)

      // Check semantic equality (parse back to numbers)
      BigDecimal(unpacked) shouldBe BigDecimal(expectedStr)
  }

  test("unpack String from BOOLEAN types") {
    // Test boolean to String conversion
    val testCases = Seq((true, "true"), (false, "false"))

    for (booleanValue, expectedStr) <- testCases do
      val packer = MessagePack.newPacker()
      packer.packBoolean(booleanValue)
      val packed   = packer.toByteArray
      val unpacked = ObjectWeaver.unweave[String](packed)
      unpacked shouldBe expectedStr
  }

  test("unpack String from NIL type") {
    // Test nil to String conversion (nil = null)
    val packer = MessagePack.newPacker()
    packer.packNil
    val packed   = packer.toByteArray
    val unpacked = ObjectWeaver.unweave[String](packed)
    unpacked shouldBe null
  }

  test("unpack String from unsupported types") {
    // Test types that cannot be converted to String
    val packer = MessagePack.newPacker()
    packer.packArrayHeader(2)
    packer.packInt(1)
    packer.packInt(2)
    val packed = packer.toByteArray

    intercept[IllegalArgumentException] {
      ObjectWeaver.unweave[String](packed)
    }
  }

  test("JSON serialization") {
    // Test JSON roundtrip
    val testCases = Seq(
      "",
      "hello",
      "1234",
      "特殊文字",
      "line1\nline2" // multiline
    )

    for str <- testCases do
      val json     = ObjectWeaver.toJson(str)
      val unpacked = ObjectWeaver.fromJson[String](json)
      unpacked shouldBe str
  }

end StringWeaverTest
