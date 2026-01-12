package wvlet.uni.design

import wvlet.uni.test.UniTest
import wvlet.uni.test.empty
import wvlet.uni.test.defined

/**
  */
object DefaultValueTest:
  // This type of default values often used in configuration classes
  case class A(a: Long = 10, b: Long = 100, c: Long = 1000)

  case class B(a: A)

/**
  */
class DefaultValueTest extends UniTest:
  import DefaultValueTest.*
  test("populate default values") {
    Design
      .newSilentDesign
      .build[B] { b =>
        b.a shouldBe A()
      }
  }
