package wvlet.ai.util

import wvlet.airspec.AirSpec

class SourceCodeTest extends AirSpec:
  test("embed the source code name and location") {
    val sc = SourceCode()
    sc.fileName shouldBe "SourceCodeTest.scala"
    sc.line shouldBe 7
    sc.col shouldBe 25
  }
