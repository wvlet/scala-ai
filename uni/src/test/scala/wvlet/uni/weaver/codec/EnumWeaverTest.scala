package wvlet.uni.weaver.codec

import wvlet.uni.test.UniTest
import wvlet.uni.weaver.Weaver

enum Color derives Weaver:
  case Red,
    Green,
    Blue

enum Priority(val level: Int) derives Weaver:
  case Low    extends Priority(1)
  case Medium extends Priority(2)
  case High   extends Priority(3)

// Enum with custom toString to verify productPrefix-based serialization
enum StatusCode(val code: Int) derives Weaver:
  case OK       extends StatusCode(200)
  case NotFound extends StatusCode(404)
  case Error    extends StatusCode(500)
  override def toString: String = s"${productPrefix}(${code})"

case class Task(name: String, priority: Priority, color: Color) derives Weaver

class EnumWeaverTest extends UniTest:

  test("roundtrip simple enum") {
    val v       = Color.Red
    val msgpack = Weaver.weave(v)
    val v2      = Weaver.unweave[Color](msgpack)
    v2 shouldBe v
  }

  test("roundtrip all simple enum values") {
    for value <- Color.values do
      val msgpack = Weaver.weave(value)
      val v2      = Weaver.unweave[Color](msgpack)
      v2 shouldBe value
  }

  test("simple enum to/from JSON") {
    val json = Weaver.toJson(Color.Green)
    json shouldBe "\"Green\""
    val v2 = Weaver.fromJson[Color](json)
    v2 shouldBe Color.Green
  }

  test("roundtrip enum with parameters") {
    val v       = Priority.High
    val msgpack = Weaver.weave(v)
    val v2      = Weaver.unweave[Priority](msgpack)
    v2 shouldBe v
    v2.level shouldBe 3
  }

  test("enum with parameters to/from JSON") {
    val json = Weaver.toJson(Priority.Medium)
    json shouldBe "\"Medium\""
    val v2 = Weaver.fromJson[Priority](json)
    v2 shouldBe Priority.Medium
    v2.level shouldBe 2
  }

  test("case class with enum fields") {
    val task    = Task("Fix bug", Priority.High, Color.Red)
    val msgpack = Weaver.weave(task)
    val v2      = Weaver.unweave[Task](msgpack)
    v2 shouldBe task
  }

  test("case class with enum fields to/from JSON") {
    val task = Task("Review PR", Priority.Medium, Color.Green)
    val json = Weaver.toJson(task)
    json shouldContain "\"priority\":\"Medium\""
    json shouldContain "\"color\":\"Green\""
    val v2 = Weaver.fromJson[Task](json)
    v2 shouldBe task
  }

  test("error for unknown enum value") {
    val e = intercept[IllegalArgumentException] {
      Weaver.fromJson[Color]("\"Purple\"")
    }
    e.getMessage shouldContain "Unknown enum value"
    e.getMessage shouldContain "Purple"
  }

  test("error for wrong type") {
    val e = intercept[IllegalArgumentException] {
      Weaver.fromJson[Color]("123")
    }
    e.getMessage shouldContain "expected STRING"
  }

  test("list of enums") {
    val colors  = List(Color.Red, Color.Green, Color.Blue)
    val msgpack = Weaver.weave(colors)
    val v2      = Weaver.unweave[List[Color]](msgpack)
    v2 shouldBe colors
  }

  test("roundtrip enum with custom toString") {
    val v    = StatusCode.NotFound
    val json = Weaver.toJson(v)
    json shouldBe "\"NotFound\""
    val v2 = Weaver.fromJson[StatusCode](json)
    v2 shouldBe v
    v2.code shouldBe 404
  }

  test("Option[Enum] in case class") {
    case class OptColor(color: Option[Color]) derives Weaver

    val v1 = OptColor(Some(Color.Red))
    val m1 = Weaver.weave(v1)
    Weaver.unweave[OptColor](m1) shouldBe v1

    val v2 = OptColor(None)
    val m2 = Weaver.weave(v2)
    Weaver.unweave[OptColor](m2) shouldBe v2
  }

end EnumWeaverTest
