package wvlet.ai.design.di

import wvlet.airspec.AirSpec
import wvlet.ai.design.Design
import wvlet.ai.surface.Surface

object AbstractTypeTest extends AirSpec {

  trait Abst {
    def hello = "hello abst"
  }
  class AbstImpl extends Abst {
    override def hello: String = "hello impl"
  }

  test("bind to abstract type") {
    val d = Design.newSilentDesign
      .bind[Abst].to[AbstImpl]

    val s = Surface.of[AbstImpl]
    s.objectFactory shouldBe defined

    d.build[Abst] { (a: Abst) =>
      a.hello shouldBe "hello impl"
    }
  }

}
