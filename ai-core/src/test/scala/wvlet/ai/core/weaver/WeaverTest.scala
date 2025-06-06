package wvlet.ai.core.weaver

import wvlet.airspec.AirSpec
import scala.jdk.CollectionConverters.*

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

  test("weave Seq[Int]") {
    val v       = Seq(1, 2, 3, 4, 5)
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[Seq[Int]](msgpack)
    v shouldBe v2
  }

  test("weave empty Seq[Int]") {
    val v       = Seq.empty[Int]
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[Seq[Int]](msgpack)
    v shouldBe v2
  }

  test("weave Seq[String]") {
    val v       = Seq("hello", "world", "test")
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[Seq[String]](msgpack)
    v shouldBe v2
  }

  test("Seq[Int] toJson") {
    val v    = Seq(1, 2, 3)
    val json = ObjectWeaver.toJson(v)
    val v2   = ObjectWeaver.fromJson[Seq[Int]](json)
    v shouldBe v2
  }

  test("nested Seq[Seq[Int]]") {
    val v       = Seq(Seq(1, 2), Seq(3, 4), Seq(5))
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[Seq[Seq[Int]]](msgpack)
    v shouldBe v2
  }

  test("weave IndexedSeq[Int]") {
    val v       = IndexedSeq(1, 2, 3, 4, 5)
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[IndexedSeq[Int]](msgpack)
    v shouldBe v2
  }

  test("weave empty IndexedSeq[Int]") {
    val v       = IndexedSeq.empty[Int]
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[IndexedSeq[Int]](msgpack)
    v shouldBe v2
  }

  test("weave IndexedSeq[String]") {
    val v       = IndexedSeq("hello", "world", "test")
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[IndexedSeq[String]](msgpack)
    v shouldBe v2
  }

  test("IndexedSeq[Int] toJson") {
    val v    = IndexedSeq(1, 2, 3)
    val json = ObjectWeaver.toJson(v)
    val v2   = ObjectWeaver.fromJson[IndexedSeq[Int]](json)
    v shouldBe v2
  }

  test("nested IndexedSeq[IndexedSeq[Int]]") {
    val v       = IndexedSeq(IndexedSeq(1, 2), IndexedSeq(3, 4), IndexedSeq(5))
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[IndexedSeq[IndexedSeq[Int]]](msgpack)
    v shouldBe v2
  }

  test("weave java.util.List[Int]") {
    val v       = List(1, 2, 3, 4, 5).asJava
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[java.util.List[Int]](msgpack)
    v2.asScala shouldBe v.asScala
  }

  test("weave empty java.util.List[Int]") {
    val v       = List.empty[Int].asJava
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[java.util.List[Int]](msgpack)
    v2.asScala shouldBe v.asScala
  }

  test("weave java.util.List[String]") {
    val v       = List("hello", "world", "test").asJava
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[java.util.List[String]](msgpack)
    v2.asScala shouldBe v.asScala
  }

  test("java.util.List[Int] toJson") {
    val v    = List(1, 2, 3).asJava
    val json = ObjectWeaver.toJson(v)
    val v2   = ObjectWeaver.fromJson[java.util.List[Int]](json)
    v2.asScala shouldBe v.asScala
  }

  test("nested java.util.List[java.util.List[Int]]") {
    val v       = List(List(1, 2).asJava, List(3, 4).asJava, List(5).asJava).asJava
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[java.util.List[java.util.List[Int]]](msgpack)
    v2.asScala.map(_.asScala) shouldBe v.asScala.map(_.asScala)
  }

  test("handle malformed array data gracefully for Seq") {
    import wvlet.ai.core.msgpack.spi.MessagePack
    // Create a malformed msgpack array with wrong element count
    val packer = MessagePack.newPacker()
    packer.packArrayHeader(3)    // Say we have 3 elements
    packer.packInt(1)            // Valid first element
    packer.packString("invalid") // Invalid second element for Seq[Int]
    packer.packInt(3)            // Third element that should be skipped

    val malformedMsgpack = packer.toByteArray

    val result =
      try
        ObjectWeaver.unweave[Seq[Int]](malformedMsgpack)
        None
      catch
        case e: Exception =>
          Some(e)

    result.isDefined shouldBe true
    result.get.getMessage.contains("Cannot convert") shouldBe true
  }

  test("handle malformed array data gracefully for IndexedSeq") {
    import wvlet.ai.core.msgpack.spi.MessagePack
    // Create a malformed msgpack array with wrong element count
    val packer = MessagePack.newPacker()
    packer.packArrayHeader(3)    // Say we have 3 elements
    packer.packInt(1)            // Valid first element
    packer.packString("invalid") // Invalid second element for IndexedSeq[Int]
    packer.packInt(3)            // Third element that should be skipped

    val malformedMsgpack = packer.toByteArray

    val result =
      try
        ObjectWeaver.unweave[IndexedSeq[Int]](malformedMsgpack)
        None
      catch
        case e: Exception =>
          Some(e)

    result.isDefined shouldBe true
    result.get.getMessage.contains("Cannot convert") shouldBe true
  }

  test("handle malformed array data gracefully for java.util.List") {
    import wvlet.ai.core.msgpack.spi.MessagePack
    // Create a malformed msgpack array with wrong element count
    val packer = MessagePack.newPacker()
    packer.packArrayHeader(3)    // Say we have 3 elements
    packer.packInt(1)            // Valid first element
    packer.packString("invalid") // Invalid second element for java.util.List[Int]
    packer.packInt(3)            // Third element that should be skipped

    val malformedMsgpack = packer.toByteArray

    val result =
      try
        ObjectWeaver.unweave[java.util.List[Int]](malformedMsgpack)
        None
      catch
        case e: Exception =>
          Some(e)

    result.isDefined shouldBe true
    result.get.getMessage.contains("Cannot convert") shouldBe true
  }

  test("weave ListMap[String, Int]") {
    val v       = scala.collection.immutable.ListMap("a" -> 1, "b" -> 2, "c" -> 3)
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[scala.collection.immutable.ListMap[String, Int]](msgpack)
    v shouldBe v2
    // Verify order is preserved
    v.keys.toList shouldBe v2.keys.toList
    v.values.toList shouldBe v2.values.toList
  }

  test("weave empty ListMap[String, Int]") {
    val v       = scala.collection.immutable.ListMap.empty[String, Int]
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[scala.collection.immutable.ListMap[String, Int]](msgpack)
    v shouldBe v2
  }

  test("weave ListMap[Int, String]") {
    val v       = scala.collection.immutable.ListMap(1 -> "one", 2 -> "two", 3 -> "three")
    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[scala.collection.immutable.ListMap[Int, String]](msgpack)
    v shouldBe v2
    // Verify order is preserved
    v.keys.toList shouldBe v2.keys.toList
    v.values.toList shouldBe v2.values.toList
  }

  test("ListMap[String, Int] toJson") {
    val v    = scala.collection.immutable.ListMap("x" -> 10, "y" -> 20, "z" -> 30)
    val json = ObjectWeaver.toJson(v)
    val v2   = ObjectWeaver.fromJson[scala.collection.immutable.ListMap[String, Int]](json)
    v shouldBe v2
    // Verify order is preserved
    v.keys.toList shouldBe v2.keys.toList
    v.values.toList shouldBe v2.values.toList
  }

  test("nested ListMap[String, List[Int]]") {
    val v = scala
      .collection
      .immutable
      .ListMap("numbers" -> List(1, 2, 3), "more" -> List(4, 5), "empty" -> List.empty[Int])
    val msgpack = ObjectWeaver.weave(v)
    val v2 = ObjectWeaver.unweave[scala.collection.immutable.ListMap[String, List[Int]]](msgpack)
    v shouldBe v2
    // Verify order is preserved
    v.keys.toList shouldBe v2.keys.toList
  }

  test("nested ListMap[String, ListMap[String, Int]]") {
    val v = scala
      .collection
      .immutable
      .ListMap(
        "group1" -> scala.collection.immutable.ListMap("a" -> 1, "b" -> 2),
        "group2" -> scala.collection.immutable.ListMap("x" -> 10, "y" -> 20),
        "empty"  -> scala.collection.immutable.ListMap.empty[String, Int]
      )
    val msgpack = ObjectWeaver.weave(v)
    val v2 = ObjectWeaver.unweave[
      scala.collection.immutable.ListMap[String, scala.collection.immutable.ListMap[String, Int]]
    ](msgpack)
    v shouldBe v2
    // Verify order is preserved for outer map
    v.keys.toList shouldBe v2.keys.toList
    // Verify order is preserved for inner maps
    v("group1").keys.toList shouldBe v2("group1").keys.toList
    v("group2").keys.toList shouldBe v2("group2").keys.toList
  }

  test("ListMap preserves insertion order") {
    // Create ListMap with specific order
    val builder  = scala.collection.immutable.ListMap.newBuilder[String, Int]
    builder += ("third"  -> 3)
    builder += ("first"  -> 1)
    builder += ("second" -> 2)
    val v        = builder.result()

    val msgpack = ObjectWeaver.weave(v)
    val v2      = ObjectWeaver.unweave[scala.collection.immutable.ListMap[String, Int]](msgpack)

    // Verify values are correct
    v shouldBe v2
    // Verify insertion order is preserved
    v.keys.toList shouldBe List("third", "first", "second")
    v2.keys.toList shouldBe List("third", "first", "second")
    v.values.toList shouldBe List(3, 1, 2)
    v2.values.toList shouldBe List(3, 1, 2)
  }

  test("handle malformed ListMap data gracefully") {
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
        ObjectWeaver.unweave[scala.collection.immutable.ListMap[String, Int]](malformedMsgpack)
        None
      catch
        case e: Exception =>
          Some(e)

    result.isDefined shouldBe true
  }

  test("handle malformed ListMap value gracefully") {
    import wvlet.ai.core.msgpack.spi.MessagePack
    // Create a malformed msgpack map with wrong value type
    val packer = MessagePack.newPacker()
    packer.packMapHeader(2)      // Say we have 2 key-value pairs
    packer.packString("key1")    // Valid first key
    packer.packInt(1)            // Valid first value
    packer.packString("key2")    // Valid second key
    packer.packString("invalid") // Invalid second value for ListMap[String, Int]

    val malformedMsgpack = packer.toByteArray

    val result =
      try
        ObjectWeaver.unweave[scala.collection.immutable.ListMap[String, Int]](malformedMsgpack)
        None
      catch
        case e: Exception =>
          Some(e)

    result.isDefined shouldBe true
    result.get.getMessage.contains("Cannot convert") shouldBe true
  }

end WeaverTest
