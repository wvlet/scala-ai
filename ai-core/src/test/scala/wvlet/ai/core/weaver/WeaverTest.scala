package wvlet.ai.core.weaver

import wvlet.airspec.AirSpec

class WeaverTest extends AirSpec:

  test("weave int") {
    val v       = 1
    val msgpack = ObjectWeaver.weave(1)
    val v2      = ObjectWeaver.unweave[Int](msgpack)
    v shouldBe v2
  }

  test("toJson") {
    val v    = 1
    val json = ObjectWeaver.toJson(1)
    val v2   = ObjectWeaver.fromJson[Int](json)
    v shouldBe v2
  }

  test("weave string") {
    val v       = "hello"
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[String](msgpack)
    v shouldBe v2
  }

  test("string toJson") {
    val v    = "hello world"
    val json = ObjectWeaver.toJson(v)
    val v2   = ObjectWeaver.fromJson[String](json)
    v shouldBe v2
  }

  test("weave List[Int]") {
    val v       = List(1, 2, 3, 4, 5)
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[List[Int]](msgpack)
    v shouldBe v2
  }

  test("weave empty List[Int]") {
    val v       = List.empty[Int]
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[List[Int]](msgpack)
    v shouldBe v2
  }

  test("weave List[String]") {
    val v       = List("hello", "world", "test")
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[List[String]](msgpack)
    v shouldBe v2
  }

  test("List[Int] toJson") {
    val v    = List(1, 2, 3)
    val json = ObjectWeaver.toJson(v)
    val v2   = ObjectWeaver.fromJson[List[Int]](json)
    v shouldBe v2
  }

  test("nested List[List[Int]]") {
    val v       = List(List(1, 2), List(3, 4), List(5))
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[List[List[Int]]](msgpack)
    v shouldBe v2
  }

  test("handle malformed array data gracefully") {
    import wvlet.ai.core.msgpack.spi.MessagePack
    // Create a malformed msgpack array with wrong element count
    val packer = MessagePack.newPacker()
    packer.packArrayHeader(3)    // Say we have 3 elements
    packer.packInt(1)            // Valid first element
    packer.packString("invalid") // Invalid second element for List[Int]
    packer.packInt(3)            // Third element that should be skipped

    val malformedMsgpack = packer.toByteArray

    val result =
      try
        ObjectWeaver.unweave[List[Int]](malformedMsgpack)
        None
      catch
        case e: Exception =>
          Some(e)

    result.isDefined shouldBe true
    result.get.getMessage.contains("Cannot convert") shouldBe true
  }

  test("weave Map[String, Int]") {
    val v       = Map("a" -> 1, "b" -> 2, "c" -> 3)
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[Map[String, Int]](msgpack)
    v shouldBe v2
  }

  test("weave empty Map[String, Int]") {
    val v       = Map.empty[String, Int]
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[Map[String, Int]](msgpack)
    v shouldBe v2
  }

  test("weave Map[Int, String]") {
    val v       = Map(1 -> "one", 2 -> "two", 3 -> "three")
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[Map[Int, String]](msgpack)
    v shouldBe v2
  }

  test("Map[String, Int] toJson") {
    val v    = Map("x" -> 10, "y" -> 20)
    val json = ObjectWeaver.toJson(v)
    val v2   = ObjectWeaver.fromJson[Map[String, Int]](json)
    v shouldBe v2
  }

  test("nested Map[String, List[Int]]") {
    val v       = Map("numbers" -> List(1, 2, 3), "more" -> List(4, 5), "empty" -> List.empty[Int])
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[Map[String, List[Int]]](msgpack)
    v shouldBe v2
  }

  test("nested Map[String, Map[String, Int]]") {
    val v = Map(
      "group1" -> Map("a" -> 1, "b" -> 2),
      "group2" -> Map("x" -> 10, "y" -> 20),
      "empty"  -> Map.empty[String, Int]
    )
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[Map[String, Map[String, Int]]](msgpack)
    v shouldBe v2
  }

  test("handle malformed map data gracefully") {
    import wvlet.ai.core.msgpack.spi.MessagePack
    // Create a malformed msgpack where we claim there are more pairs than we provide
    val packer = MessagePack.newPacker()
    packer.packMapHeader(3)   // Say we have 3 key-value pairs
    packer.packString("key1") // Valid first key
    packer.packInt(1)         // Valid first value
    packer.packString("key2") // Valid second key
    packer.packInt(2)         // Valid second value
    // Missing third key-value pair!

    val malformedMsgpack = packer.toByteArray

    val result =
      try
        ObjectWeaver.unweave[Map[String, Int]](malformedMsgpack)
        None
      catch
        case e: Exception =>
          Some(e)

    result.isDefined shouldBe true
  }

  test("handle malformed map value gracefully") {
    import wvlet.ai.core.msgpack.spi.MessagePack
    // Create a malformed msgpack map with wrong value type
    val packer = MessagePack.newPacker()
    packer.packMapHeader(2)      // Say we have 2 key-value pairs
    packer.packString("key1")    // Valid first key
    packer.packInt(1)            // Valid first value
    packer.packString("key2")    // Valid second key
    packer.packString("invalid") // Invalid second value for Map[String, Int]

    val malformedMsgpack = packer.toByteArray

    val result =
      try
        ObjectWeaver.unweave[Map[String, Int]](malformedMsgpack)
        None
      catch
        case e: Exception =>
          Some(e)

    result.isDefined shouldBe true
    result.get.getMessage.contains("Cannot convert") shouldBe true
  }

end WeaverTest
