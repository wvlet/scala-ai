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
      val packer = MessagePack.newPacker()
      packer.packDouble(floatValue)
      val packed   = packer.toByteArray
      val unpacked = ObjectWeaver.unweave[Int](packed)
      unpacked shouldBe expectedInt
  }

  test("unpack Int from FLOAT types - failure cases") {
    // Test floats that cannot be converted to Int
    val invalidFloats = Seq(1.5, -2.7, Double.PositiveInfinity, Double.NegativeInfinity, Double.NaN)

    for floatValue <- invalidFloats do
      val packer = MessagePack.newPacker()
      packer.packDouble(floatValue)
      val packed = packer.toByteArray

      val exception = intercept[IllegalArgumentException] {
        ObjectWeaver.unweave[Int](packed)
      }
      exception.getMessage.contains("Cannot convert double") shouldBe true
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
      val packer = MessagePack.newPacker()
      packer.packString(stringValue)
      val packed   = packer.toByteArray
      val unpacked = ObjectWeaver.unweave[Int](packed)
      unpacked shouldBe expectedInt
  }

  test("unpack Int from STRING types - failure cases") {
    // Test invalid strings
    val invalidStrings = Seq("hello", "1.5", "", "2147483648", "-2147483649", "0x10")

    for stringValue <- invalidStrings do
      val packer = MessagePack.newPacker()
      packer.packString(stringValue)
      val packed = packer.toByteArray

      val exception = intercept[IllegalArgumentException] {
        ObjectWeaver.unweave[Int](packed)
      }
      exception.getMessage.contains("Cannot convert string") shouldBe true
  }

  test("unpack Int from BOOLEAN types") {
    // Test boolean to int conversion (true = 1, false = 0)
    val packer1 = MessagePack.newPacker()
    packer1.packBoolean(true)
    val packed1   = packer1.toByteArray
    val unpacked1 = ObjectWeaver.unweave[Int](packed1)
    unpacked1 shouldBe 1

    val packer2 = MessagePack.newPacker()
    packer2.packBoolean(false)
    val packed2   = packer2.toByteArray
    val unpacked2 = ObjectWeaver.unweave[Int](packed2)
    unpacked2 shouldBe 0
  }

  test("unpack Int from NIL type") {
    // Test nil should result in null context, which returns 0 as default
    val packer = MessagePack.newPacker()
    packer.packNil
    val packed   = packer.toByteArray
    val unpacked = ObjectWeaver.unweave[Int](packed)
    unpacked shouldBe 0
  }

  test("unpack Int from unsupported types") {
    // Test types that cannot be converted to Int
    val packer = MessagePack.newPacker()
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
      val packer = MessagePack.newPacker()
      packer.packLong(longValue)
      val packed = packer.toByteArray

      intercept[Exception] {
        ObjectWeaver.unweave[Int](packed)
      }
  }

  test("Long weaver basic operations") {
    val longValues = Seq(0L, 1L, -1L, Long.MaxValue, Long.MinValue, 9223372036854775807L)

    for longValue <- longValues do
      val packed   = ObjectWeaver.weave(longValue)
      val unpacked = ObjectWeaver.unweave[Long](packed)
      unpacked shouldBe longValue
  }

  test("Long weaver from various types") {
    // From boolean
    val packerBool = MessagePack.newPacker()
    packerBool.packBoolean(true)
    val packedBool   = packerBool.toByteArray
    val unpackedBool = ObjectWeaver.unweave[Long](packedBool)
    unpackedBool shouldBe 1L

    // From string
    val packerStr = MessagePack.newPacker()
    packerStr.packString("42")
    val packedStr   = packerStr.toByteArray
    val unpackedStr = ObjectWeaver.unweave[Long](packedStr)
    unpackedStr shouldBe 42L

    // From nil (should return default value for Long type)
    val packerNil = MessagePack.newPacker()
    packerNil.packNil
    val packedNil   = packerNil.toByteArray
    val unpackedNil = ObjectWeaver.unweave[Long](packedNil)
    unpackedNil shouldBe 0L

    // From float (whole number)
    val packerFloat = MessagePack.newPacker()
    packerFloat.packDouble(123.0)
    val packedFloat   = packerFloat.toByteArray
    val unpackedFloat = ObjectWeaver.unweave[Long](packedFloat)
    unpackedFloat shouldBe 123L
  }

  test("Double weaver basic operations") {
    val doubleValues = Seq(0.0, 1.0, -1.0, 3.14159, Double.MaxValue, Double.MinValue)

    for doubleValue <- doubleValues do
      val packed   = ObjectWeaver.weave(doubleValue)
      val unpacked = ObjectWeaver.unweave[Double](packed)
      unpacked shouldBe doubleValue
  }

  test("Double weaver from various types") {
    // From integer
    val packerInt = MessagePack.newPacker()
    packerInt.packLong(42L)
    val packedInt   = packerInt.toByteArray
    val unpackedInt = ObjectWeaver.unweave[Double](packedInt)
    unpackedInt shouldBe 42.0

    // From string
    val packerStr = MessagePack.newPacker()
    packerStr.packString("3.14")
    val packedStr   = packerStr.toByteArray
    val unpackedStr = ObjectWeaver.unweave[Double](packedStr)
    unpackedStr shouldBe 3.14

    // From boolean
    val packerBool = MessagePack.newPacker()
    packerBool.packBoolean(false)
    val packedBool   = packerBool.toByteArray
    val unpackedBool = ObjectWeaver.unweave[Double](packedBool)
    unpackedBool shouldBe 0.0

    // From nil (should return default value for Double type)
    val packerNil = MessagePack.newPacker()
    packerNil.packNil
    val packedNil   = packerNil.toByteArray
    val unpackedNil = ObjectWeaver.unweave[Double](packedNil)
    unpackedNil shouldBe 0.0
  }

  test("Float weaver basic operations") {
    val floatValues = Seq(0.0f, 1.0f, -1.0f, 3.14f, Float.MaxValue, Float.MinValue)

    for floatValue <- floatValues do
      val packed   = ObjectWeaver.weave(floatValue)
      val unpacked = ObjectWeaver.unweave[Float](packed)
      unpacked shouldBe floatValue
  }

  test("Boolean weaver basic operations") {
    val boolValues = Seq(true, false)

    for boolValue <- boolValues do
      val packed   = ObjectWeaver.weave(boolValue)
      val unpacked = ObjectWeaver.unweave[Boolean](packed)
      unpacked shouldBe boolValue
  }

  test("Boolean weaver from various types") {
    // From integer - non-zero is true
    val packerInt = MessagePack.newPacker()
    packerInt.packInt(5)
    val packedInt   = packerInt.toByteArray
    val unpackedInt = ObjectWeaver.unweave[Boolean](packedInt)
    unpackedInt shouldBe true

    // From integer - zero is false
    val packerZero = MessagePack.newPacker()
    packerZero.packInt(0)
    val packedZero   = packerZero.toByteArray
    val unpackedZero = ObjectWeaver.unweave[Boolean](packedZero)
    unpackedZero shouldBe false

    // From string - various true values
    val trueStrings = Seq("true", "1", "yes", "on", "TRUE", "YES", "ON")
    for trueStr <- trueStrings do
      val packer = MessagePack.newPacker()
      packer.packString(trueStr)
      val packed   = packer.toByteArray
      val unpacked = ObjectWeaver.unweave[Boolean](packed)
      unpacked shouldBe true

    // From string - various false values
    val falseStrings = Seq("false", "0", "no", "off", "", "FALSE", "NO", "OFF")
    for falseStr <- falseStrings do
      val packer = MessagePack.newPacker()
      packer.packString(falseStr)
      val packed   = packer.toByteArray
      val unpacked = ObjectWeaver.unweave[Boolean](packed)
      unpacked shouldBe false
  }

  test("Byte weaver basic operations") {
    val byteValues = Seq(0.toByte, 1.toByte, -1.toByte, Byte.MaxValue, Byte.MinValue)

    for byteValue <- byteValues do
      val packed   = ObjectWeaver.weave(byteValue)
      val unpacked = ObjectWeaver.unweave[Byte](packed)
      unpacked shouldBe byteValue
  }

  test("Byte weaver range validation") {
    // Test value outside byte range
    val packer = MessagePack.newPacker()
    packer.packInt(300) // Outside byte range
    val packed = packer.toByteArray

    val exception = intercept[IllegalArgumentException] {
      ObjectWeaver.unweave[Byte](packed)
    }
    exception.getMessage.contains("out of Byte range") shouldBe true
  }

  test("Short weaver basic operations") {
    val shortValues = Seq(0.toShort, 1.toShort, -1.toShort, Short.MaxValue, Short.MinValue)

    for shortValue <- shortValues do
      val packed   = ObjectWeaver.weave(shortValue)
      val unpacked = ObjectWeaver.unweave[Short](packed)
      unpacked shouldBe shortValue
  }

  test("Short weaver range validation") {
    // Test value outside short range
    val packer = MessagePack.newPacker()
    packer.packInt(50000) // Outside short range
    val packed = packer.toByteArray

    intercept[Exception] {
      ObjectWeaver.unweave[Short](packed)
    }
  }

  test("Char weaver basic operations") {
    val charValues = Seq('a', 'Z', '0', '9', ' ', '\n', '\u0000', '\uFFFF')

    for charValue <- charValues do
      val packed   = ObjectWeaver.weave(charValue)
      val unpacked = ObjectWeaver.unweave[Char](packed)
      unpacked shouldBe charValue
  }

  test("Char weaver from string") {
    // Single character string
    val packer = MessagePack.newPacker()
    packer.packString("A")
    val packed   = packer.toByteArray
    val unpacked = ObjectWeaver.unweave[Char](packed)
    unpacked shouldBe 'A'
  }

  test("Char weaver from string - failure cases") {
    // Multi-character string should fail
    val packer = MessagePack.newPacker()
    packer.packString("AB")
    val packed = packer.toByteArray

    intercept[Exception] {
      ObjectWeaver.unweave[Char](packed)
    }
  }

  test("Char weaver from integer") {
    // Character code conversion
    val packer = MessagePack.newPacker()
    packer.packInt(65) // ASCII 'A'
    val packed   = packer.toByteArray
    val unpacked = ObjectWeaver.unweave[Char](packed)
    unpacked shouldBe 'A'
  }

  test("Primitive type error handling") {
    // Test unsupported conversions for each type
    val packer = MessagePack.newPacker()
    packer.packArrayHeader(1)
    packer.packInt(1)
    val packed = packer.toByteArray

    val primitiveTypes = Seq(
      () => ObjectWeaver.unweave[Long](packed),
      () => ObjectWeaver.unweave[Double](packed),
      () => ObjectWeaver.unweave[Float](packed),
      () => ObjectWeaver.unweave[Boolean](packed),
      () => ObjectWeaver.unweave[Byte](packed),
      () => ObjectWeaver.unweave[Short](packed),
      () => ObjectWeaver.unweave[Char](packed)
    )

    for unpackOperation <- primitiveTypes do
      intercept[Exception] {
        unpackOperation()
      }
  }

end PrimitiveWeaverTest
