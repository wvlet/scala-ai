package wvlet.uni.weaver

import wvlet.uni.test.UniTest

// Test sealed trait with case class children
sealed trait Animal derives Weaver
case class Dog(name: String, age: Int)      extends Animal derives Weaver
case class Cat(name: String, color: String) extends Animal derives Weaver

// Test sealed trait with case object children
sealed trait Status derives Weaver
case object Active                 extends Status derives Weaver
case object Inactive               extends Status derives Weaver
case class Pending(reason: String) extends Status derives Weaver

// Test nested objects within sealed trait children
case class Owner(name: String) derives Weaver
sealed trait Pet derives Weaver
case class DogPet(name: String, owner: Owner) extends Pet derives Weaver
case class CatPet(name: String, age: Int)     extends Pet derives Weaver

class SealedTraitWeaverTest extends UniTest:

  test("roundtrip case class children") {
    val dog: Animal = Dog("Rex", 5)
    val msgpack     = Weaver.weave(dog)
    val restored    = Weaver.unweave[Animal](msgpack)
    restored shouldBe dog

    val cat: Animal = Cat("Whiskers", "orange")
    val msgpack2    = Weaver.weave(cat)
    val restored2   = Weaver.unweave[Animal](msgpack2)
    restored2 shouldBe cat
  }

  test("roundtrip case objects") {
    val active: Status = Active
    val msgpack        = Weaver.weave(active)
    val restored       = Weaver.unweave[Status](msgpack)
    restored shouldBe Active

    val inactive: Status = Inactive
    val msgpack2         = Weaver.weave(inactive)
    val restored2        = Weaver.unweave[Status](msgpack2)
    restored2 shouldBe Inactive
  }

  test("JSON format with discriminator") {
    val dog: Animal = Dog("Rex", 5)
    val json        = Weaver.toJson(dog)
    json shouldContain "\"@type\":\"Dog\""
    json shouldContain "\"name\":\"Rex\""
    json shouldContain "\"age\":5"
  }

  test("JSON format for case object") {
    val active: Status = Active
    val json           = Weaver.toJson(active)
    json shouldBe """{"@type":"Active"}"""
  }

  test("parse JSON with discriminator") {
    val json     = """{"@type":"Dog","name":"Buddy","age":3}"""
    val restored = Weaver.fromJson[Animal](json)
    restored shouldBe Dog("Buddy", 3)
  }

  test("parse JSON case object") {
    val json     = """{"@type":"Active"}"""
    val restored = Weaver.fromJson[Status](json)
    restored shouldBe Active
  }

  test("custom discriminator field name") {
    val config = WeaverConfig(discriminatorFieldName = "type")
    val dog    = Dog("Rex", 5)
    val weaver = summon[Weaver[Animal]]
    val json   = weaver.toJson(dog, config)
    json shouldContain "\"type\":\"Dog\""

    val restored = weaver.fromJson(json, config)
    restored shouldBe dog
  }

  test("nested objects within sealed trait children") {
    val pet: Pet = DogPet("Buddy", Owner("Alice"))
    val json     = Weaver.toJson(pet)
    json shouldContain "\"@type\":\"DogPet\""
    json shouldContain "\"owner\""
    json shouldContain "\"Alice\""

    val restored = Weaver.fromJson[Pet](json)
    restored shouldBe pet
  }

  test("mixed case class and case object children") {
    val pending: Status = Pending("awaiting approval")
    val json            = Weaver.toJson(pending)
    json shouldContain "\"@type\":\"Pending\""
    json shouldContain "\"reason\":\"awaiting approval\""

    val restored = Weaver.fromJson[Status](json)
    restored shouldBe pending
  }

  test("error: missing discriminator") {
    val json = """{"name":"Rex","age":5}"""
    val e    = intercept[IllegalArgumentException] {
      Weaver.fromJson[Animal](json)
    }
    e.getMessage shouldContain "Missing discriminator field"
  }

  test("error: unknown type name") {
    val json = """{"@type":"Bird","name":"Tweety"}"""
    val e    = intercept[IllegalArgumentException] {
      Weaver.fromJson[Animal](json)
    }
    e.getMessage shouldContain "Unknown type"
    e.getMessage shouldContain "Dog"
    e.getMessage shouldContain "Cat"
  }

  test("canonical name matching - lowercase type") {
    val json     = """{"@type":"dog","name":"Rex","age":5}"""
    val restored = Weaver.fromJson[Animal](json)
    restored shouldBe Dog("Rex", 5)
  }

  test("canonical name matching - uppercase type") {
    val json     = """{"@type":"DOG","name":"Rex","age":5}"""
    val restored = Weaver.fromJson[Animal](json)
    restored shouldBe Dog("Rex", 5)
  }

  test("canonical name matching - snake_case type") {
    // DogPet should match dog_pet
    val json     = """{"@type":"dog_pet","name":"Buddy","owner":{"name":"Alice"}}"""
    val restored = Weaver.fromJson[Pet](json)
    restored shouldBe DogPet("Buddy", Owner("Alice"))
  }

  test("field order independence") {
    // Discriminator at end
    val json1     = """{"name":"Rex","age":5,"@type":"Dog"}"""
    val restored1 = Weaver.fromJson[Animal](json1)
    restored1 shouldBe Dog("Rex", 5)

    // Discriminator in middle
    val json2     = """{"name":"Whiskers","@type":"Cat","color":"orange"}"""
    val restored2 = Weaver.fromJson[Animal](json2)
    restored2 shouldBe Cat("Whiskers", "orange")
  }

  test("null value handling") {
    val weaver   = summon[Weaver[Animal]]
    val context  = WeaverContext()
    val msgpack  = wvlet.uni.msgpack.spi.MessagePack.fromJSON("null")
    val unpacker = wvlet.uni.msgpack.spi.MessagePack.newUnpacker(msgpack)
    weaver.unpack(unpacker, context)
    context.isNull shouldBe true
  }

end SealedTraitWeaverTest
