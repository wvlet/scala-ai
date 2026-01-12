package wvlet.uni.design

import wvlet.airspec.AirSpec

/**
  */
object DependencyTest1:
  class A(val b: B)
  class B(val c: C)
  case class C(d: D)

  trait D
  class DImpl extends D

class DependencyTest extends AirSpec:

  test("show missing dependencies") {
    val d = Design.newSilentDesign
    d.withSession { session =>
      val m = intercept[DesignException] {
        val a = session.build[DependencyTest1.A]
      }
      m.code shouldBe DesignErrorCode.MISSING_DEPENDENCY
      val msg = m.getMessage
      msg.contains("D <- C") shouldBe true
    }
  }

  test("resolve concrete dependencies") {
    val d = Design
      .newSilentDesign
      .bindImpl[DependencyTest1.D, DependencyTest1.DImpl] // abstract class to a concrete trait
    d.withSession { session =>
      val a = session.build[DependencyTest1.A]
    }
  }
