package wvlet.ai.core.weaver

import wvlet.airspec.AirSpec

class WeaverTest extends AirSpec:

  test("weave int") {
    val v       = 1
    val msgpack = ObjectWeaver.weave(1)
    val v2      = ObjectWeaver.unweave(msgpack)
    v shouldBe v2
  }
