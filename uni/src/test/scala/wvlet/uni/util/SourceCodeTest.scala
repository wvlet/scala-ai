package wvlet.uni.util

import wvlet.uni.util.SourceCode
import wvlet.uni.test.UniTest
import wvlet.uni.test.empty
import wvlet.uni.test.defined

class SourceCodeTest extends UniTest:
  test("embed the source code name and location") {
    val sc = SourceCode()
    sc.fileName shouldBe "SourceCodeTest.scala"
    sc.line shouldBe 10
    sc.col shouldBe 25
  }
