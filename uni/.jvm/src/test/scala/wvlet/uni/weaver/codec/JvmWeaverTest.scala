package wvlet.uni.weaver.codec

import wvlet.uni.test.UniTest
import wvlet.uni.weaver.Weaver
import wvlet.uni.weaver.codec.JvmWeaver.given
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
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
