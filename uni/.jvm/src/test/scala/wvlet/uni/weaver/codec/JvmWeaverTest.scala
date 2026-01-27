package wvlet.uni.weaver.codec

import wvlet.uni.test.UniTest
import wvlet.uni.weaver.Weaver
import wvlet.uni.weaver.codec.JvmWeaver.given
import java.io.File
import java.net.URI
import java.net.URL
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime

class JvmWeaverTest extends UniTest:

  // ====== ZonedDateTime ======

  test("roundtrip ZonedDateTime") {
    val v       = ZonedDateTime.parse("2024-01-15T10:30:00+09:00[Asia/Tokyo]")
    val msgpack = Weaver.weave(v)
    val v2      = Weaver.unweave[ZonedDateTime](msgpack)
    v2 shouldBe v
  }

  test("ZonedDateTime to/from JSON") {
    val v    = ZonedDateTime.parse("2024-01-15T10:30:00+09:00[Asia/Tokyo]")
    val json = Weaver.toJson(v)
    val v2   = Weaver.fromJson[ZonedDateTime](json)
    v2 shouldBe v
  }

  test("ZonedDateTime UTC") {
    val v       = ZonedDateTime.parse("2024-06-01T00:00:00Z")
    val msgpack = Weaver.weave(v)
    val v2      = Weaver.unweave[ZonedDateTime](msgpack)
    v2 shouldBe v
  }

  // ====== LocalDate ======

  test("roundtrip LocalDate") {
    val v       = LocalDate.parse("2024-01-15")
    val msgpack = Weaver.weave(v)
    val v2      = Weaver.unweave[LocalDate](msgpack)
    v2 shouldBe v
  }

  test("LocalDate to/from JSON") {
    val v    = LocalDate.parse("2024-12-25")
    val json = Weaver.toJson(v)
    json shouldBe "\"2024-12-25\""
    val v2 = Weaver.fromJson[LocalDate](json)
    v2 shouldBe v
  }

  // ====== LocalDateTime ======

  test("roundtrip LocalDateTime") {
    val v       = LocalDateTime.parse("2024-01-15T10:30:00")
    val msgpack = Weaver.weave(v)
    val v2      = Weaver.unweave[LocalDateTime](msgpack)
    v2 shouldBe v
  }

  test("LocalDateTime to/from JSON") {
    val v    = LocalDateTime.parse("2024-06-15T12:30:45")
    val json = Weaver.toJson(v)
    val v2   = Weaver.fromJson[LocalDateTime](json)
    v2 shouldBe v
  }

  test("LocalDateTime with nanoseconds") {
    val v       = LocalDateTime.parse("2024-01-15T10:30:00.123456789")
    val msgpack = Weaver.weave(v)
    val v2      = Weaver.unweave[LocalDateTime](msgpack)
    v2 shouldBe v
  }

  // ====== Duration ======

  test("roundtrip Duration") {
    val v       = Duration.ofHours(2).plusMinutes(30)
    val msgpack = Weaver.weave(v)
    val v2      = Weaver.unweave[Duration](msgpack)
    v2 shouldBe v
  }

  test("Duration to/from JSON") {
    val v    = Duration.ofSeconds(90)
    val json = Weaver.toJson(v)
    json shouldBe "\"PT1M30S\""
    val v2 = Weaver.fromJson[Duration](json)
    v2 shouldBe v
  }

  test("Duration zero") {
    val v       = Duration.ZERO
    val msgpack = Weaver.weave(v)
    val v2      = Weaver.unweave[Duration](msgpack)
    v2 shouldBe v
  }

  // ====== Composite: case class with JVM types ======

  case class Event(
      date: LocalDate,
      time: LocalDateTime,
      zonedTime: ZonedDateTime,
      duration: Duration
  ) derives Weaver

  test("case class with JVM time types") {
    val v = Event(
      date = LocalDate.parse("2024-01-15"),
      time = LocalDateTime.parse("2024-01-15T10:30:00"),
      zonedTime = ZonedDateTime.parse("2024-01-15T10:30:00+09:00[Asia/Tokyo]"),
      duration = Duration.ofMinutes(45)
    )
    val msgpack = Weaver.weave(v)
    val v2      = Weaver.unweave[Event](msgpack)
    v2 shouldBe v
  }

  // ====== OffsetDateTime ======

  test("roundtrip OffsetDateTime") {
    val v       = OffsetDateTime.parse("2024-01-15T10:30:00+09:00")
    val msgpack = Weaver.weave(v)
    val v2      = Weaver.unweave[OffsetDateTime](msgpack)
    v2 shouldBe v
  }

  test("OffsetDateTime to/from JSON") {
    val v    = OffsetDateTime.parse("2024-06-15T12:00:00Z")
    val json = Weaver.toJson(v)
    val v2   = Weaver.fromJson[OffsetDateTime](json)
    v2 shouldBe v
  }

  test("OffsetDateTime with nanoseconds") {
    val v       = OffsetDateTime.parse("2024-01-15T10:30:00.123456789+05:30")
    val msgpack = Weaver.weave(v)
    val v2      = Weaver.unweave[OffsetDateTime](msgpack)
    v2 shouldBe v
  }

  test("OffsetDateTime invalid string") {
    val e = intercept[IllegalArgumentException] {
      Weaver.fromJson[OffsetDateTime]("\"not-a-date\"")
    }
    e.getMessage shouldContain "OffsetDateTime"
  }

  // ====== File ======

  test("roundtrip File") {
    val v       = File("/tmp/test.txt")
    val msgpack = Weaver.weave(v)
    val v2      = Weaver.unweave[File](msgpack)
    v2 shouldBe v
  }

  test("File to/from JSON") {
    val v    = File("/home/user/docs")
    val json = Weaver.toJson(v)
    json shouldBe "\"/home/user/docs\""
    val v2 = Weaver.fromJson[File](json)
    v2 shouldBe v
  }

  // ====== URL ======

  test("roundtrip URL") {
    val v       = URI("https://example.com/path").toURL
    val msgpack = Weaver.weave(v)
    val v2      = Weaver.unweave[URL](msgpack)
    v2 shouldBe v
  }

  test("URL to/from JSON") {
    val v    = URI("https://example.com:8080/api").toURL
    val json = Weaver.toJson(v)
    json shouldBe "\"https://example.com:8080/api\""
    val v2 = Weaver.fromJson[URL](json)
    v2 shouldBe v
  }

  // ====== java.nio.file.Path ======

  test("roundtrip Path") {
    val v       = java.nio.file.Path.of("/tmp/test.txt")
    val msgpack = Weaver.weave(v)
    val v2      = Weaver.unweave[java.nio.file.Path](msgpack)
    v2 shouldBe v
  }

  test("Path to/from JSON") {
    val v    = java.nio.file.Path.of("/home/user/docs")
    val json = Weaver.toJson(v)
    json shouldBe "\"/home/user/docs\""
    val v2 = Weaver.fromJson[java.nio.file.Path](json)
    v2 shouldBe v
  }

  test("Path relative") {
    val v       = java.nio.file.Path.of("relative/path/file.txt")
    val msgpack = Weaver.weave(v)
    val v2      = Weaver.unweave[java.nio.file.Path](msgpack)
    v2 shouldBe v
  }

  // ====== java.util.Optional ======

  test("roundtrip Optional with value") {
    val v       = java.util.Optional.of("hello")
    val msgpack = Weaver.weave(v)
    val v2      = Weaver.unweave[java.util.Optional[String]](msgpack)
    v2 shouldBe v
  }

  test("roundtrip Optional empty") {
    val v       = java.util.Optional.empty[String]()
    val msgpack = Weaver.weave(v)
    val v2      = Weaver.unweave[java.util.Optional[String]](msgpack)
    v2.isPresent shouldBe false
  }

  test("Optional[Int] to/from JSON") {
    val v    = java.util.Optional.of(42)
    val json = Weaver.toJson(v)
    json shouldBe "42"
    val v2 = Weaver.fromJson[java.util.Optional[Int]](json)
    v2 shouldBe v
  }

  test("Optional empty to/from JSON") {
    val v    = java.util.Optional.empty[Int]()
    val json = Weaver.toJson(v)
    json shouldBe "null"
    val v2 = Weaver.fromJson[java.util.Optional[Int]](json)
    v2.isPresent shouldBe false
  }

  // ====== Error handling ======

  test("ZonedDateTime invalid string") {
    val e = intercept[IllegalArgumentException] {
      Weaver.fromJson[ZonedDateTime]("\"not-a-date\"")
    }
    e.getMessage shouldContain "ZonedDateTime"
  }

  test("LocalDate invalid string") {
    val e = intercept[IllegalArgumentException] {
      Weaver.fromJson[LocalDate]("\"not-a-date\"")
    }
    e.getMessage shouldContain "LocalDate"
  }

end JvmWeaverTest
