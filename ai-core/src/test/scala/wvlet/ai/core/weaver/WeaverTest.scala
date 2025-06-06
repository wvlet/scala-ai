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

end WeaverTest
