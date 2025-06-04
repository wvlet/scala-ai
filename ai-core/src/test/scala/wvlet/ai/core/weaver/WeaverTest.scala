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
