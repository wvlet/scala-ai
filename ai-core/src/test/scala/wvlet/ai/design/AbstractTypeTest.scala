package wvlet.ai.design

import wvlet.ai.core.surface.Surface
import wvlet.ai.design.Design
import wvlet.airspec.AirSpec

object AbstractTypeTest extends AirSpec:

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
