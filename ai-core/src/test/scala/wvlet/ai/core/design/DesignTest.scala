package wvlet.ai.core.design

import wvlet.ai.core.surface.Surface
import DesignTest.Alias.{HelloRef, StringHello}
import wvlet.airspec.AirSpec

/**
  */
object DesignTest extends AirSpec:

  trait Message
  case class Hello(message: String) extends Message

  object Alias:
    trait Hello[A]:
      def hello: A

    class StringHello extends Hello[String]:
      def hello = "hello world"

    type HelloRef = Hello[String]

  type ProductionMessage  = Message
  type DevelopmentMessage = Message
  type ProductionString   = String

  val d0 = Design.empty

  val d1 =
    d0.bindImpl[Message, Hello]
      .bindInstance[Hello](Hello("world"))
      .bindSingleton[Message]
      // TODO Skip evaluating this if it was overridden in the Session.init phase
      // .bindEagerSingleton[Message]
      .bindInstance[ProductionMessage](Hello("production"))
      .bindInstance[DevelopmentMessage](Hello("development"))
      .noLifeCycleLogging

  val o = Hello("override")

  test("be immutable") {
    d0 shouldBe Design.empty

    val d2 = d1.bindInstance[Hello](Hello("airframe"))
    d2 shouldNotBe d1
  }

  test("be appendable") {
    val d2 = d1.bindInstance[Hello](o)

    val d3 = d1 + d2
    val d4 = d1.add(d2)

    d3.build[Hello] { h =>
      h shouldBeTheSameInstanceAs o
    }
    d4.build[Hello] { h =>
      h shouldBeTheSameInstanceAs o
    }
  }

  test("display design") {
    val s = d1.toString
    // sanity test
    s shouldNotBe empty
    debug(d1.toString)
  }

  test("remove binding") {
    val dd = d1.remove[Message]

    def hasMessage(d: Design): Boolean = d.binding.exists(_.from == Surface.of[Message])
    def hasProductionMessage(d: Design): Boolean = d
      .binding
      .exists(_.from == Surface.of[ProductionMessage])

    hasMessage(d1) shouldBe true
    hasMessage(dd) shouldBe false

    hasProductionMessage(d1) shouldBe true
    hasProductionMessage(dd) shouldBe true
  }

  test("bind providers") {
    // TODO: Remove type argument when https://github.com/wvlet/airframe/issues/2200 fixed
    val d = Design
      .newSilentDesign
      .bindProvider[ProductionString, Hello] { m =>
        Hello(m)
      }
      .bindInstance[ProductionString]("hello production")

    d.build[Hello] { h =>
      h.message shouldBe "hello production"
    }
  }

  test("bind type aliases") {
    val d = Design.newSilentDesign.bindInstance[HelloRef](new StringHello)

    d.build[HelloRef] { h =>
      h.hello shouldBe "hello world"
    }
  }

  test("start and stop session") {
    // Sanity test
    Design
      .newDesign
      .noLifeCycleLogging
      .withSession { session =>
        // Do nothing
      }
  }

  test("set/unset options") {
    val d = Design.newDesign
    d.withTracer(DefaultTracer).noTracer
  }

  test("preserve explicit design options") {
    val d1 = Design.newDesign.withProductionMode.noLifeCycleLogging
    val d2 = Design.newDesign

    d1.designOptions.enabledLifeCycleLogging shouldBe Some(false)
    d1.designOptions.stage shouldBe Some(Stage.PRODUCTION)

    d2.designOptions.enabledLifeCycleLogging shouldBe empty
    d2.designOptions.stage shouldBe empty

    val d = d1 + d2
    d.designOptions.enabledLifeCycleLogging shouldBe Some(false)
    d.designOptions.stage shouldBe Some(Stage.PRODUCTION)

    val d3 = d2 + d1
    d3.designOptions.enabledLifeCycleLogging shouldBe Some(false)
    d3.designOptions.stage shouldBe Some(Stage.PRODUCTION)
  }

  test("override design options") {
    val d1 = Design.newDesign.withProductionMode.noLifeCycleLogging
    val d2 = Design.newDesign.withLazyMode.withLifeCycleLogging
    val d  = d1 + d2
    d.designOptions.enabledLifeCycleLogging shouldBe Some(true)
    d.designOptions.stage shouldBe Some(Stage.DEVELOPMENT)
  }

  test("support run") {
    val d = Design.newSilentDesign.bindInstance[String]("hello")
    val ret = d.run { (s: String) =>
      s shouldBe "hello"
      100
    }
    ret shouldBe 100
  }

  test("find outer variables in code block") {
    val helloDesign = "hello"
    val d           = Design.newSilentDesign.bindInstance[String](helloDesign)

    d.build[String] { x =>
      helloDesign
    }
  }

end DesignTest
