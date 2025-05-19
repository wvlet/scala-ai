package wvlet.ai.core.weaver

import wvlet.airspec.AirSpec

class WeaverTest extends AirSpec:

  test("weave int") {
    val v       = 1
    val msgpack = ObjectWeaver.weave(1)
    val v2: Int = ObjectWeaver.unweave[Int](msgpack)
    v shouldBe v2
  }

  test("toJson") {
    val v       = 1
    val json    = ObjectWeaver.toJson(1)
    val v2: Int = ObjectWeaver.fromJson[Int](json)
    v shouldBe v2
  }
