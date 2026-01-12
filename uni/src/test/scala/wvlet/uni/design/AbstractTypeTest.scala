package wvlet.uni.design

import wvlet.uni.surface.Surface
import wvlet.uni.test.UniTest
import wvlet.uni.test.empty
import wvlet.uni.test.defined

object AbstractTypeTest extends UniTest:

  trait Abst:
    def hello = "hello abst"

  class AbstImpl extends Abst:
    override def hello: String = "hello impl"

  test("bind to abstract type") {
    val d = Design.newSilentDesign.bindImpl[Abst, AbstImpl]

    val s = Surface.of[AbstImpl]
    s.objectFactory shouldBe defined

    d.build[Abst] { (a: Abst) =>
      a.hello shouldBe "hello impl"
    }
  }
