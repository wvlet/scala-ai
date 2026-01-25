package wvlet.uni.weaver

import wvlet.uni.test.UniTest

// Test sealed trait with case class children
sealed trait Animal derives ObjectWeaver
case class Dog(name: String, age: Int)      extends Animal derives ObjectWeaver
case class Cat(name: String, color: String) extends Animal derives ObjectWeaver

// Test sealed trait with case object children
sealed trait Status derives ObjectWeaver
case object Active                 extends Status derives ObjectWeaver
case object Inactive               extends Status derives ObjectWeaver
case class Pending(reason: String) extends Status derives ObjectWeaver

// Test nested objects within sealed trait children
case class Owner(name: String) derives ObjectWeaver
sealed trait Pet derives ObjectWeaver
case class DogPet(name: String, owner: Owner) extends Pet derives ObjectWeaver
case class CatPet(name: String, age: Int)     extends Pet derives ObjectWeaver

class SealedTraitWeaverTest extends UniTest:

  test("roundtrip case class children") {
    val dog: Animal = Dog("Rex", 5)
    val msgpack     = ObjectWeaver.weave(dog)
    val restored    = ObjectWeaver.unweave[Animal](msgpack)
    restored shouldBe dog

    val cat: Animal = Cat("Whiskers", "orange")
    val msgpack2    = ObjectWeaver.weave(cat)
    val restored2   = ObjectWeaver.unweave[Animal](msgpack2)
    restored2 shouldBe cat
  }

  test("roundtrip case objects") {
    val active: Status = Active
    val msgpack        = ObjectWeaver.weave(active)
    val restored       = ObjectWeaver.unweave[Status](msgpack)
    restored shouldBe Active

    val inactive: Status = Inactive
    val msgpack2         = ObjectWeaver.weave(inactive)
    val restored2        = ObjectWeaver.unweave[Status](msgpack2)
    restored2 shouldBe Inactive
  }

  test("JSON format with discriminator") {
    val dog: Animal = Dog("Rex", 5)
    val json        = ObjectWeaver.toJson(dog)
    json shouldContain "\"@type\":\"Dog\""
    json shouldContain "\"name\":\"Rex\""
    json shouldContain "\"age\":5"
  }

  test("JSON format for case object") {
    val active: Status = Active
    val json           = ObjectWeaver.toJson(active)
    json shouldBe """{"@type":"Active"}"""
  }

  test("parse JSON with discriminator") {
    val json     = """{"@type":"Dog","name":"Buddy","age":3}"""
    val restored = ObjectWeaver.fromJson[Animal](json)
    restored shouldBe Dog("Buddy", 3)
  }

  test("parse JSON case object") {
    val json     = """{"@type":"Active"}"""
    val restored = ObjectWeaver.fromJson[Status](json)
    restored shouldBe Active
  }

  test("custom discriminator field name") {
    val config = WeaverConfig(discriminatorFieldName = "type")
    val dog    = Dog("Rex", 5)
    val weaver = summon[ObjectWeaver[Animal]]
    val json   = weaver.toJson(dog, config)
    json shouldContain "\"type\":\"Dog\""

    val restored = weaver.fromJson(json, config)
    restored shouldBe dog
  }

  test("nested objects within sealed trait children") {
    val pet: Pet = DogPet("Buddy", Owner("Alice"))
    val json     = ObjectWeaver.toJson(pet)
    json shouldContain "\"@type\":\"DogPet\""
    json shouldContain "\"owner\""
    json shouldContain "\"Alice\""

    val restored = ObjectWeaver.fromJson[Pet](json)
    restored shouldBe pet
  }

  test("mixed case class and case object children") {
    val pending: Status = Pending("awaiting approval")
    val json            = ObjectWeaver.toJson(pending)
    json shouldContain "\"@type\":\"Pending\""
    json shouldContain "\"reason\":\"awaiting approval\""

    val restored = ObjectWeaver.fromJson[Status](json)
    restored shouldBe pending
  }

  test("error: missing discriminator") {
    val json   = """{"name":"Rex","age":5}"""
    val result =
      try
        ObjectWeaver.fromJson[Animal](json)
        None
      catch
        case e: Exception =>
          Some(e)
    result.isDefined shouldBe true
    result.get.getMessage shouldContain "Missing discriminator field"
  }

  test("error: unknown type name") {
    val json   = """{"@type":"Bird","name":"Tweety"}"""
    val result =
      try
        ObjectWeaver.fromJson[Animal](json)
        None
      catch
        case e: Exception =>
          Some(e)
    result.isDefined shouldBe true
    result.get.getMessage shouldContain "Unknown type"
    result.get.getMessage shouldContain "Dog"
    result.get.getMessage shouldContain "Cat"
  }

  test("canonical name matching - lowercase type") {
    val json     = """{"@type":"dog","name":"Rex","age":5}"""
    val restored = ObjectWeaver.fromJson[Animal](json)
    restored shouldBe Dog("Rex", 5)
  }

  test("canonical name matching - uppercase type") {
    val json     = """{"@type":"DOG","name":"Rex","age":5}"""
    val restored = ObjectWeaver.fromJson[Animal](json)
    restored shouldBe Dog("Rex", 5)
  }

  test("canonical name matching - snake_case type") {
    // DogPet should match dog_pet
    val json     = """{"@type":"dog_pet","name":"Buddy","owner":{"name":"Alice"}}"""
    val restored = ObjectWeaver.fromJson[Pet](json)
    restored shouldBe DogPet("Buddy", Owner("Alice"))
  }

  test("field order independence") {
    // Discriminator at end
    val json1     = """{"name":"Rex","age":5,"@type":"Dog"}"""
    val restored1 = ObjectWeaver.fromJson[Animal](json1)
    restored1 shouldBe Dog("Rex", 5)

    // Discriminator in middle
    val json2     = """{"name":"Whiskers","@type":"Cat","color":"orange"}"""
    val restored2 = ObjectWeaver.fromJson[Animal](json2)
    restored2 shouldBe Cat("Whiskers", "orange")
  }

  test("null value handling") {
    val weaver   = summon[ObjectWeaver[Animal]]
    val context  = WeaverContext()
    val msgpack  = wvlet.uni.msgpack.spi.MessagePack.fromJSON("null")
    val unpacker = wvlet.uni.msgpack.spi.MessagePack.newUnpacker(msgpack)
    weaver.unpack(unpacker, context)
    context.isNull shouldBe true
  }

end SealedTraitWeaverTest
