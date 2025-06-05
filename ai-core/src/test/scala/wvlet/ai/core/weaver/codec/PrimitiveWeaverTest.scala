package wvlet.ai.core.weaver.codec

import wvlet.ai.core.msgpack.spi.MessagePack
import wvlet.ai.core.weaver.ObjectWeaver
import wvlet.airspec.AirSpec

class PrimitiveWeaverTest extends AirSpec:

  test("unpack Int from INTEGER types") {
    // Test various integer formats
    val intValues = Seq(0, 1, -1, 127, -128, 32767, -32768, Int.MaxValue, Int.MinValue)

    for intValue <- intValues do
      val packed   = ObjectWeaver.weave(intValue)
      val unpacked = ObjectWeaver.unweave[Int](packed)
      unpacked shouldBe intValue
  }

  test("unpack Int from FLOAT types") {
    // Test whole number floats that can be converted to Int
    val testCases = Seq(
      (1.0, 1),
      (0.0, 0),
      (-1.0, -1),
      (42.0, 42),
      (Int.MaxValue.toDouble, Int.MaxValue),
      (Int.MinValue.toDouble, Int.MinValue)
    )

    for (floatValue, expectedInt) <- testCases do
      val packer = MessagePack.newPacker
      packer.packDouble(floatValue)
      val packed   = packer.toByteArray
      val unpacked = ObjectWeaver.unweave[Int](packed)
      unpacked shouldBe expectedInt
  }

  test("unpack Int from FLOAT types - failure cases") {
    // Test floats that cannot be converted to Int
    val invalidFloats = Seq(1.5, -2.7, Double.PositiveInfinity, Double.NegativeInfinity, Double.NaN)

    for floatValue <- invalidFloats do
      val packer = MessagePack.newPacker
      packer.packDouble(floatValue)
      val packed = packer.toByteArray

      intercept[Exception] {
        ObjectWeaver.unweave[Int](packed)
      }
  }

  test("unpack Int from STRING types") {
    // Test valid numeric strings
    val testCases = Seq(
      ("0", 0),
      ("1", 1),
      ("-1", -1),
      ("42", 42),
      ("2147483647", Int.MaxValue),
      ("-2147483648", Int.MinValue)
    )

    for (stringValue, expectedInt) <- testCases do
      val packer = MessagePack.newPacker
      packer.packString(stringValue)
      val packed   = packer.toByteArray
      val unpacked = ObjectWeaver.unweave[Int](packed)
      unpacked shouldBe expectedInt
  }

  test("unpack Int from STRING types - failure cases") {
    // Test invalid strings
    val invalidStrings = Seq("hello", "1.5", "", "2147483648", "-2147483649", "0x10")

    for stringValue <- invalidStrings do
      val packer = MessagePack.newPacker
      packer.packString(stringValue)
      val packed = packer.toByteArray

      intercept[Exception] {
        ObjectWeaver.unweave[Int](packed)
      }
  }

  test("unpack Int from BOOLEAN types") {
    // Test boolean to int conversion (true = 1, false = 0)
    val packer1 = MessagePack.newPacker
    packer1.packBoolean(true)
    val packed1   = packer1.toByteArray
    val unpacked1 = ObjectWeaver.unweave[Int](packed1)
    unpacked1 shouldBe 1

    val packer2 = MessagePack.newPacker
    packer2.packBoolean(false)
    val packed2   = packer2.toByteArray
    val unpacked2 = ObjectWeaver.unweave[Int](packed2)
    unpacked2 shouldBe 0
  }

  test("unpack Int from NIL type") {
    // Test nil to int conversion (nil = 0)
    val packer = MessagePack.newPacker
    packer.packNil
    val packed   = packer.toByteArray
    val unpacked = ObjectWeaver.unweave[Int](packed)
    unpacked shouldBe 0
  }

  test("unpack Int from unsupported types") {
    // Test types that cannot be converted to Int
    val packer = MessagePack.newPacker
    packer.packArrayHeader(2)
    packer.packInt(1)
    packer.packInt(2)
    val packed = packer.toByteArray

    intercept[Exception] {
      ObjectWeaver.unweave[Int](packed)
    }
  }

  test("large integer edge cases") {
    // Test integers that are at the edge of Int range
    val testCases = Seq(
      Long.MaxValue,           // Should fail - too large for Int
      Long.MinValue,           // Should fail - too small for Int
      Int.MaxValue.toLong + 1, // Should fail - overflow
      Int.MinValue.toLong - 1  // Should fail - underflow
    )

    for longValue <- testCases do
      val packer = MessagePack.newPacker
      packer.packLong(longValue)
      val packed = packer.toByteArray

      intercept[Exception] {
        ObjectWeaver.unweave[Int](packed)
      }
  }

end PrimitiveWeaverTest
