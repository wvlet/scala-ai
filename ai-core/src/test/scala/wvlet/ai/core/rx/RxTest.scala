/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.ai.core.rx

import wvlet.ai.core.design.Design
import wvlet.airspec.AirSpec

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

/**
  */
object RxTest extends AirSpec:

  /**
    * TODO This test needs to be rewritten to utilize async testing of AirSpec.
    * @param rx
    * @tparam A
    * @return
    */
  private def eval[A](rx: Rx[A]): Seq[RxEvent] =
    val b = Seq.newBuilder[RxEvent]
    RxRunner.run(rx)(b += _)
    val events = b.result()
    debug(events.mkString(", "))
    events

  test("create a new Rx variable") {
    val v = Rx.variable(1)
    v.toString
    v.get shouldBe 1

    val rx: Rx[String] = v.map(x => s"count: ${x}").withName("sample rx")
    val updateCount    = new AtomicInteger(0)
    val value          = new AtomicReference[String]()
    val subscription = rx.subscribe { (x: String) =>
      updateCount.incrementAndGet()
      value.set(x)
    }
    // The subscription must be executed upon the registration
    updateCount.get() shouldBe 1
    value.get() shouldBe s"count: 1"

    // Propagate changes
    v := 2
    updateCount.get() shouldBe 2
    value.get() shouldBe s"count: 2"

    // If the value is unchanged, it should not propergate update
    v := 2
    updateCount.get() shouldBe 2
    value.get() shouldBe s"count: 2"

    // If we cancel the subscription, it will no longer propagate the change
    subscription.cancel

    v := 3
    updateCount.get() shouldBe 2
    value.get() shouldBe s"count: 2"
  }

  test("rx variable") {
    val v = Rx.variable(1)
    v.toString
    v.parents shouldBe empty
    v.get shouldBe 1
    v := 2
    v.get shouldBe 2
  }

  test("bind rx", design = _.bind[RxVar[String]].toInstance(Rx.variable("Hello"))) {
    (v: RxVar[String]) =>
      v.get shouldBe "Hello"
  }

  test("force update RxVar") {

    val v     = Rx.variable(1)
    val count = new AtomicInteger(0)
    v.subscribe(newValue => count.incrementAndGet())

    v.get shouldBe 1
    count.get() shouldBe 1
    v.set(1)
    v.get shouldBe 1
    count.get() shouldBe 1

    v.forceSet(1)
    v.get shouldBe 1
    count.get() shouldBe 2

    v.forceUpdate(x => x * 2)
    v.get shouldBe 2
    count.get() shouldBe 3

    v.forceUpdate(x => x)
    v.get shouldBe 2
    count.get() shouldBe 4

    v.update(x => x)
    v.get shouldBe 2
    count.get() shouldBe 4
  }

  test("chain Rx operators") {
    val v  = Rx.const(2)
    val v1 = v.map(_ + 1)
    val v2 = v1.flatMap(i => Rx.const(i * 2))
    val op = v2.withName("multiply")

    // Parents
    v.parents shouldBe empty
    v1.parents.find(_ eq v) shouldBe defined
    v2.parents.find(_ eq v1) shouldBe defined
    op.parents.find(_ eq v2) shouldBe defined

    // toString sanity test
    v.toString
    v1.toString
    v2.toString
    op.toString

    // Run chain
    val c1 = v.run { v =>
      v shouldBe 2
    }
    c1.cancel
    val c2 = v1.run { v =>
      v shouldBe 3
    }
    c2.cancel
    val c3 = v2.run { v =>
      v shouldBe 6
    }
    c3.cancel
    val c4 = op.run { v =>
      v shouldBe 6
    }
    c4.cancel
  }

  test("filter") {
    val v = Rx.const(1)
    v.filter(_ == 1)
      .run { v =>
        v shouldBe 1
      }
    v.filter(_ != 1)
      .run { v =>
        fail("cannot reach here")
      }
  }

  test("filter with Option[X]") {
    val v = Rx.const(Some(10))
    v.filter(_.isDefined)
      .map(_.get)
      .run { x =>
        x shouldBe 10
      }

    val n = Rx.const[Option[Int]](None)
    n.filter(_.isDefined)
      .map(_.get)
      .run { x =>
        fail("cannot reach here")
      }
  }

  test("for-comprehension") {
    val v =
      for (
        x <- Rx.const(1)
        if x == 1
      )
        yield x
    v.run(x => x shouldBe 1)

    val n =
      for (
        x <- Rx.const(1)
        if x != 1
      )
        yield x
    n.run(x => fail("cannot reach here"))
  }

  test("flatMap with RxOption") {
    Rx.const(1)
      .flatMap { v =>
        Rx.option(Some(v))
      }
      .map {
        case Some(x) =>
          x shouldBe 1
        case _ =>
          fail("can't reach here")
      }
  }

  test("zip") {
    val a = Rx.variable(1)
    val b = Rx.variable("a")

    val x     = a.zip(b)
    var count = 0

    val c = x.run { v =>
      count match
        case 0 =>
          v shouldBe (1, "a")
        case 1 =>
          v shouldBe (2, "b")
        case _ =>
          fail(s"unexpected value ${v}")
    }

    count += 1
    a := 2
    b := "b"
    c.cancel
  }

  test("zip sequences") {
    val a = Rx.sequence(1, 2, 3)
    val b = Rx.sequence("a", "b")

    val r = Seq.newBuilder[(Int, String)]
    a.zip(b).run(r += _)

    r.result() shouldBe Seq((1, "a"), (2, "b"))

    val r2 = Seq.newBuilder[(String, Int)]
    b.zip(a).run(r2 += _)
    r2.result() shouldBe Seq(("a", 1), ("b", 2))
  }

  test("zip3") {
    val a = Rx.variable(1)
    val b = Rx.variable("a")
    val c = Rx.variable(true)

    val x     = a.zip(b, c)
    var count = 0

    val e = x.run { v =>
      count match
        case 0 =>
          v shouldBe (1, "a", true)
        case 1 =>
          v shouldBe (2, "b", false)
        case _ =>
          fail(s"unexpected value: ${v}")
    }

    count += 1
    a := 2
    b := "b"
    c := false
    e.cancel
  }

  test("zip4") {
    val a = Rx.variable(1)
    val b = Rx.variable("a")
    val c = Rx.variable(true)
    val d = Rx.variable(false)

    val x  = a.zip(b, c, d)
    val ev = Seq.newBuilder[RxEvent]
    val ca = RxRunner.runContinuously(x)(ev += _)

    a := 2
    b := "b"
    c := false
    d := true

    val events = ev.result()
    events shouldBe Seq(OnNext(1, "a", true, false), OnNext(2, "b", false, true))
  }

  test("zip5") {
    val a = Rx.variable(1)
    val b = Rx.variable("a")
    val c = Rx.variable(true)
    val d = Rx.variable(false)
    val e = Rx.variable(10)

    val x  = a.zip(b, c, d, e)
    val ev = Seq.newBuilder[RxEvent]
    val ca = RxRunner.runContinuously(x)(ev += _)

    a := 2
    b := "b"
    c := false
    d := true
    e := 20

    val events = ev.result()
    events shouldBe Seq(OnNext(1, "a", true, false, 10), OnNext(2, "b", false, true, 20))
  }

  test("zip6") {
    val a = Rx.variable(1)
    val b = Rx.variable("a")
    val c = Rx.variable(true)
    val d = Rx.variable(false)
    val e = Rx.variable(10)
    val f = Rx.variable("hello")

    val x  = a.zip(b, c, d, e, f)
    val ev = Seq.newBuilder[RxEvent]
    val ca = RxRunner.runContinuously(x)(ev += _)

    a := 2
    b := "b"
    c := false
    d := true
    e := 20
    f := "world"

    val events = ev.result()
    events shouldBe
      Seq(OnNext(1, "a", true, false, 10, "hello"), OnNext(2, "b", false, true, 20, "world"))
  }

  test("zip7") {
    val a = Rx.variable(1)
    val b = Rx.variable("a")
    val c = Rx.variable(true)
    val d = Rx.variable(false)
    val e = Rx.variable(10)
    val f = Rx.variable("hello")
    val g = Rx.variable(100)

    val x  = a.zip(b, c, d, e, f, g)
    val ev = Seq.newBuilder[RxEvent]
    val ca = RxRunner.runContinuously(x)(ev += _)

    a := 2
    b := "b"
    c := false
    d := true
    e := 20
    f := "world"
    g := 200

    val events = ev.result()
    events shouldBe
      Seq(
        OnNext(1, "a", true, false, 10, "hello", 100),
        OnNext(2, "b", false, true, 20, "world", 200)
      )
  }

  test("zip8") {
    val a = Rx.variable(1)
    val b = Rx.variable("a")
    val c = Rx.variable(true)
    val d = Rx.variable(false)
    val e = Rx.variable(10)
    val f = Rx.variable("hello")
    val g = Rx.variable(100)
    val h = Rx.variable(1000)

    val x  = a.zip(b, c, d, e, f, g, h)
    val ev = Seq.newBuilder[RxEvent]
    val ca = RxRunner.runContinuously(x)(ev += _)

    a := 2
    b := "b"
    c := false
    d := true
    e := 20
    f := "world"
    g := 200
    h := 2000

    val events = ev.result()
    events shouldBe
      Seq(
        OnNext(1, "a", true, false, 10, "hello", 100, 1000),
        OnNext(2, "b", false, true, 20, "world", 200, 2000)
      )
  }

  test("zip9") {
    val a = Rx.variable(1)
    val b = Rx.variable("a")
    val c = Rx.variable(true)
    val d = Rx.variable(false)
    val e = Rx.variable(10)
    val f = Rx.variable("hello")
    val g = Rx.variable(100)
    val h = Rx.variable(1000)
    val i = Rx.variable("world")

    val x  = a.zip(b, c, d, e, f, g, h, i)
    val ev = Seq.newBuilder[RxEvent]
    val ca = RxRunner.runContinuously(x)(ev += _)

    a := 2
    b := "b"
    c := false
    d := true
    e := 20
    f := "world"
    g := 200
    h := 2000
    i := "hello"

    val events = ev.result()
    events shouldBe
      Seq(
        OnNext(1, "a", true, false, 10, "hello", 100, 1000, "world"),
        OnNext(2, "b", false, true, 20, "world", 200, 2000, "hello")
      )
  }

  test("zip10") {
    val a = Rx.variable(1)
    val b = Rx.variable("a")
    val c = Rx.variable(true)
    val d = Rx.variable(false)
    val e = Rx.variable(10)
    val f = Rx.variable("hello")
    val g = Rx.variable(100)
    val h = Rx.variable(1000)
    val i = Rx.variable("world")
    val j = Rx.variable(10000)

    val x  = a.zip(b, c, d, e, f, g, h, i, j)
    val ev = Seq.newBuilder[RxEvent]
    val ca = RxRunner.runContinuously(x)(ev += _)

    a := 2
    b := "b"
    c := false
    d := true
    e := 20
    f := "world"
    g := 200
    h := 2000
    i := "hello"
    j := 100000

    val events = ev.result()
    events shouldBe
      Seq(
        OnNext(1, "a", true, false, 10, "hello", 100, 1000, "world", 10000),
        OnNext(2, "b", false, true, 20, "world", 200, 2000, "hello", 100000)
      )
  }

  test("zip Seq[Rx[A]]") {
    val a = Rx.single(1)
    val b = Rx.single(2)
    val c = Rx.single(3)
    val d = Rx.single(4)
    val e = Rx.single(5)

    Rx.zip(Seq(a, b, c, d, e))
      .map { seq =>
        seq shouldMatch { case Seq(1, 2, 3, 4, 5) =>
          debug(seq)
        }
      }
  }

  test("join") {
    val x  = Rx.variable(1)
    val y  = Rx.variable("a")
    val rx = x.join(y)

    val b = Seq.newBuilder[RxEvent]
    RxRunner.run(rx)(b += _)

    y := "b"
    y := "c"
    x := 2
    y := "d"

    val events = b.result()
    debug(events)
    events shouldBe
      Seq(OnNext(1, "a"), OnNext(1, "b"), OnNext(1, "c"), OnNext(2, "c"), OnNext(2, "d"))
  }

  test("join3") {
    val x  = Rx.variable(1)
    val y  = Rx.variable("a")
    val z  = Rx.variable(true)
    val rx = x.join(y, z)

    val b = Seq.newBuilder[RxEvent]
    RxRunner.run(rx)(b += _)

    y := "b"
    y := "c"
    z := false
    x := 2
    y := "d"

    val events = b.result()
    debug(events)
    events shouldBe
      Seq(
        OnNext(1, "a", true),
        OnNext(1, "b", true),
        OnNext(1, "c", true),
        OnNext(1, "c", false),
        OnNext(2, "c", false),
        OnNext(2, "d", false)
      )
  }

  test("join4") {
    val x  = Rx.variable(1)
    val y  = Rx.variable("a")
    val z  = Rx.variable(true)
    val w  = Rx.variable(10)
    val rx = x.join(y, z, w)

    val b = Seq.newBuilder[RxEvent]
    RxRunner.run(rx)(b += _)

    y := "b"
    y := "c"
    w := 20
    z := false
    x := 2
    y := "d"

    val events = b.result()
    debug(events)
    events shouldBe
      Seq(
        OnNext(1, "a", true, 10),
        OnNext(1, "b", true, 10),
        OnNext(1, "c", true, 10),
        OnNext(1, "c", true, 20),
        OnNext(1, "c", false, 20),
        OnNext(2, "c", false, 20),
        OnNext(2, "d", false, 20)
      )
  }

  test("join5") {
    val x  = Rx.variable(1)
    val y  = Rx.variable("a")
    val z  = Rx.variable(true)
    val w  = Rx.variable(10)
    val v  = Rx.variable(false)
    val rx = x.join(y, z, w, v)

    val b = Seq.newBuilder[RxEvent]
    RxRunner.run(rx)(b += _)

    y := "b"
    y := "c"
    w := 20
    z := false
    x := 2
    y := "d"
    v := true

    val events = b.result()
    debug(events)
    events shouldBe
      Seq(
        OnNext(1, "a", true, 10, false),
        OnNext(1, "b", true, 10, false),
        OnNext(1, "c", true, 10, false),
        OnNext(1, "c", true, 20, false),
        OnNext(1, "c", false, 20, false),
        OnNext(2, "c", false, 20, false),
        OnNext(2, "d", false, 20, false),
        OnNext(2, "d", false, 20, true)
      )
  }

  test("join6") {
    val x  = Rx.variable(1)
    val y  = Rx.variable("a")
    val z  = Rx.variable(true)
    val w  = Rx.variable(10)
    val v  = Rx.variable(false)
    val u  = Rx.variable(100)
    val rx = x.join(y, z, w, v, u)

    val b = Seq.newBuilder[RxEvent]
    RxRunner.run(rx)(b += _)

    y := "b"
    y := "c"
    w := 20
    z := false
    x := 2
    y := "d"
    v := true
    u := 200

    val events = b.result()
    debug(events)
    events shouldBe
      Seq(
        OnNext(1, "a", true, 10, false, 100),
        OnNext(1, "b", true, 10, false, 100),
        OnNext(1, "c", true, 10, false, 100),
        OnNext(1, "c", true, 20, false, 100),
        OnNext(1, "c", false, 20, false, 100),
        OnNext(2, "c", false, 20, false, 100),
        OnNext(2, "d", false, 20, false, 100),
        OnNext(2, "d", false, 20, true, 100),
        OnNext(2, "d", false, 20, true, 200)
      )
  }

  test("join7") {
    val x  = Rx.variable(1)
    val y  = Rx.variable("a")
    val z  = Rx.variable(true)
    val w  = Rx.variable(10)
    val v  = Rx.variable(false)
    val u  = Rx.variable(100)
    val t  = Rx.variable("hello")
    val rx = x.join(y, z, w, v, u, t)

    val b = Seq.newBuilder[RxEvent]
    RxRunner.run(rx)(b += _)

    y := "b"
    y := "c"
    w := 20
    z := false
    x := 2
    y := "d"
    v := true
    u := 200
    t := "world"

    val events = b.result()
    debug(events)
    events shouldBe
      Seq(
        OnNext(1, "a", true, 10, false, 100, "hello"),
        OnNext(1, "b", true, 10, false, 100, "hello"),
        OnNext(1, "c", true, 10, false, 100, "hello"),
        OnNext(1, "c", true, 20, false, 100, "hello"),
        OnNext(1, "c", false, 20, false, 100, "hello"),
        OnNext(2, "c", false, 20, false, 100, "hello"),
        OnNext(2, "d", false, 20, false, 100, "hello"),
        OnNext(2, "d", false, 20, true, 100, "hello"),
        OnNext(2, "d", false, 20, true, 200, "hello"),
        OnNext(2, "d", false, 20, true, 200, "world")
      )
  }

  test("join8") {
    val x  = Rx.variable(1)
    val y  = Rx.variable("a")
    val z  = Rx.variable(true)
    val w  = Rx.variable(10)
    val v  = Rx.variable(false)
    val u  = Rx.variable(100)
    val t  = Rx.variable("hello")
    val s  = Rx.variable(1000)
    val rx = x.join(y, z, w, v, u, t, s)

    val b = Seq.newBuilder[RxEvent]
    RxRunner.run(rx)(b += _)

    y := "b"
    y := "c"
    w := 20
    z := false
    x := 2
    y := "d"
    v := true
    u := 200
    t := "world"
    s := 2000

    val events = b.result()
    debug(events)
    events shouldBe
      Seq(
        OnNext(1, "a", true, 10, false, 100, "hello", 1000),
        OnNext(1, "b", true, 10, false, 100, "hello", 1000),
        OnNext(1, "c", true, 10, false, 100, "hello", 1000),
        OnNext(1, "c", true, 20, false, 100, "hello", 1000),
        OnNext(1, "c", false, 20, false, 100, "hello", 1000),
        OnNext(2, "c", false, 20, false, 100, "hello", 1000),
        OnNext(2, "d", false, 20, false, 100, "hello", 1000),
        OnNext(2, "d", false, 20, true, 100, "hello", 1000),
        OnNext(2, "d", false, 20, true, 200, "hello", 1000),
        OnNext(2, "d", false, 20, true, 200, "world", 1000),
        OnNext(2, "d", false, 20, true, 200, "world", 2000)
      )
  }

  test("join9") {
    val x  = Rx.variable(1)
    val y  = Rx.variable("a")
    val z  = Rx.variable(true)
    val w  = Rx.variable(10)
    val v  = Rx.variable(false)
    val u  = Rx.variable(100)
    val t  = Rx.variable("hello")
    val s  = Rx.variable(1000)
    val r  = Rx.variable(10000)
    val rx = x.join(y, z, w, v, u, t, s, r)

    val b = Seq.newBuilder[RxEvent]
    RxRunner.run(rx)(b += _)

    y := "b"
    y := "c"
    w := 20
    z := false
    x := 2
    y := "d"
    v := true
    u := 200
    t := "world"
    s := 2000
    r := 20000

    val events = b.result()
    debug(events)
    events shouldBe
      Seq(
        OnNext(1, "a", true, 10, false, 100, "hello", 1000, 10000),
        OnNext(1, "b", true, 10, false, 100, "hello", 1000, 10000),
        OnNext(1, "c", true, 10, false, 100, "hello", 1000, 10000),
        OnNext(1, "c", true, 20, false, 100, "hello", 1000, 10000),
        OnNext(1, "c", false, 20, false, 100, "hello", 1000, 10000),
        OnNext(2, "c", false, 20, false, 100, "hello", 1000, 10000),
        OnNext(2, "d", false, 20, false, 100, "hello", 1000, 10000),
        OnNext(2, "d", false, 20, true, 100, "hello", 1000, 10000),
        OnNext(2, "d", false, 20, true, 200, "hello", 1000, 10000),
        OnNext(2, "d", false, 20, true, 200, "world", 1000, 10000),
        OnNext(2, "d", false, 20, true, 200, "world", 2000, 10000),
        OnNext(2, "d", false, 20, true, 200, "world", 2000, 20000)
      )
  }

  test("join10") {
    val x  = Rx.variable(1)
    val y  = Rx.variable("a")
    val z  = Rx.variable(true)
    val w  = Rx.variable(10)
    val v  = Rx.variable(false)
    val u  = Rx.variable(100)
    val t  = Rx.variable("hello")
    val s  = Rx.variable(1000)
    val r  = Rx.variable(10000)
    val q  = Rx.variable(100000)
    val rx = x.join(y, z, w, v, u, t, s, r, q)

    val b = Seq.newBuilder[RxEvent]
    RxRunner.run(rx)(b += _)

    y := "b"
    y := "c"
    w := 20
    z := false
    x := 2
    y := "d"
    v := true
    u := 200
    t := "world"
    s := 2000
    r := 20000
    q := 200000

    val events = b.result()
    debug(events)
    events shouldBe
      Seq(
        OnNext(1, "a", true, 10, false, 100, "hello", 1000, 10000, 100000),
        OnNext(1, "b", true, 10, false, 100, "hello", 1000, 10000, 100000),
        OnNext(1, "c", true, 10, false, 100, "hello", 1000, 10000, 100000),
        OnNext(1, "c", true, 20, false, 100, "hello", 1000, 10000, 100000),
        OnNext(1, "c", false, 20, false, 100, "hello", 1000, 10000, 100000),
        OnNext(2, "c", false, 20, false, 100, "hello", 1000, 10000, 100000),
        OnNext(2, "d", false, 20, false, 100, "hello", 1000, 10000, 100000),
        OnNext(2, "d", false, 20, true, 100, "hello", 1000, 10000, 100000),
        OnNext(2, "d", false, 20, true, 200, "hello", 1000, 10000, 100000),
        OnNext(2, "d", false, 20, true, 200, "world", 1000, 10000, 100000),
        OnNext(2, "d", false, 20, true, 200, "world", 2000, 10000, 100000),
        OnNext(2, "d", false, 20, true, 200, "world", 2000, 20000, 100000),
        OnNext(2, "d", false, 20, true, 200, "world", 2000, 20000, 200000)
      )
  }

  implicit val ec: scala.concurrent.ExecutionContext = compat.defaultExecutionContext

  test("from Future[X]") {
    val f  = Future.successful(1)
    val rx = f.toRx

    rx.map { x =>
      x shouldBe 1
    }
  }

  test("from Future[Exception]") {
    val fe: Future[Exception] = Future.failed(new IllegalArgumentException)
    val rx                    = fe.toRx

    pending("Need to fix async test for RxOps in airspec")
    rx.map { x =>
      x shouldMatch { case e: IllegalArgumentException =>
      // ok
      }
    }
  }

  test("handle exception in Future[X]") {
    val p = Promise[Int]()
    val rx = Rx
      .future(p.future)
      .map(_ * 2)
      .recover { case e: IllegalArgumentException =>
        -1
      }
    rx.run { x =>
      x shouldBe -1
    }
    p.failure(new IllegalArgumentException)
      .future
      .transform {
        case Success(ret) =>
          Try(fail("cannot reach here"))
        case Failure(ex) =>
          // ok
          Success(true)
      }
  }

  test("Future response") {
    val p = Promise[Int]()
    val rx = Rx
      .future(p.future)
      .map { x =>
        x * 2
      }
    p.complete(Try(10))
    rx.run { x =>
      x shouldBe 20
    }
  }

  test("andThen(Future[X])") {
    val p  = Promise[Int]()
    val rx = Rx.const(1).andThen(x => p.future).map(_ * 2)
    p.complete(Try(10))
    rx.run(x => x shouldBe 20)
  }

  test("lastOption") {
    val rx      = Rx.sequence(1, 2, 3).lastOption
    var counter = 0
    rx.run { x =>
      counter += 1
      x shouldBe Some(3)
    }
    counter shouldBe 1
  }

  test("lastOption empty") {
    val rx      = Rx.empty[Int].lastOption
    var counter = 0
    rx.run { x =>
      counter += 1
      x shouldBe empty
    }
    counter shouldBe 1
  }

  test("take") {
    val rx = Rx.sequence(1, 2, 3, 4, 5).take(3)
    val b  = Seq.newBuilder[Int]
    rx.run {
      b += _
    }
    b.result() shouldBe Seq(1, 2, 3)
  }

  test("concat") {
    val rx = Rx.single(1).concat(Rx.single(2)).map(_ * 2)
    val b  = Seq.newBuilder[Int]
    rx.run {
      b += _
    }
    b.result() shouldBe Seq(2, 4)
  }

  test("startWith(single)") {
    val rx = Rx.single(1).startWith(0)
    eval(rx) shouldBe Seq(OnNext(0), OnNext(1), OnCompletion)
  }

  test("startWith(Seq)") {
    val rx = Rx.single(1).startWith(Seq(0, 0))
    eval(rx) shouldBe Seq(OnNext(0), OnNext(0), OnNext(1), OnCompletion)
  }

  test("sequence") {
    val rx = Rx.fromSeq(Seq(1, 2, 3)).map(_ * 2)
    val b  = Seq.newBuilder[Int]
    rx.run(b += _)
    b.result() shouldBe Seq(2, 4, 6)
  }

  test("empty") {
    val rx = Rx.empty[Int].map(_ * 2)
    val b  = Seq.newBuilder[Int]
    rx.run(b += _)
    b.result() shouldBe Seq.empty
  }

  test("recover from an error") {
    def recoveryFunction: PartialFunction[Throwable, Any] = { case e: IllegalArgumentException =>
      0
    }

    // (test name, input, expected value on success)
    def newTests(rx: Rx[Int]): Seq[(String, Rx[Any], Any)] = Seq(
      ("single", rx, Seq(1)),
      ("map", rx.map(x => x * 2), Seq(2)),
      ("flatMap", rx.flatMap(x => Rx.single(3)), Seq(3)),
      ("filter", rx.filter(_ => true), Seq(1)),
      ("zip", rx.zip(Rx.single(2)), Seq((1, 2))),
      ("zip3", rx.zip(Rx.single(2), Rx.single(3)), Seq((1, 2, 3))),
      ("concat", rx.concat(Rx.single(2)), Seq(1, 2)),
      ("lastOption", rx.lastOption, Seq(Some(1))),
      ("option Some(x)", rx.map(Some(_)).toOption, Seq(Some(1))),
      ("option None", rx.map(x => None).toOption, Seq(None))
    ).map { x =>
      (x._1, x._2.recover(recoveryFunction), x._3)
    }

    test("normal behavior") {
      for (name, t, expected) <- newTests(Rx.single(1)) do
        test(name) {
          var executed = false
          val b        = Seq.newBuilder[Any]
          t.run { x =>
            b += x
          }
          b.result() shouldBe expected
        }
    }

    test("failure recovery") {
      for (name, t, expected) <- newTests(
          Rx.single(throw new IllegalArgumentException("test failure"))
        )
      do
        test(name) {
          var executed = false
          t.run { x =>
            executed = true
            x shouldBe 0
          }
          executed shouldBe true
        }
    }
  }

  test("recover in the middle") {
    val rx = Rx
      .sequence(1, 2, 3)
      .map {
        case 2 =>
          throw new IllegalArgumentException("test error")
        case other =>
          other
      }
      .recover { case e: IllegalArgumentException =>
        -1
      }
    debug(rx)
    eval(rx) shouldBe Seq(OnNext(1), OnNext(-1), OnNext(3), OnCompletion)
  }

  test("recover in the middle failed") {
    val ex = new IllegalArgumentException("test error")
    val rx = Rx
      .sequence(1, 2, 3)
      .map {
        case 2 =>
          throw ex
        case other =>
          other
      }
      .recover { case e: IllegalStateException =>
        -1
      }
    debug(rx)
    eval(rx) shouldBe Seq(OnNext(1), OnError(ex))
  }

  test("Rx.exception") {
    val ex = new IllegalArgumentException("test error")

    val rx: Rx[Int] = Rx
      .sequence(1, 2)
      .map {
        case 1 =>
          1
        case 2 =>
          throw ex
      }
      .recoverWith { case e: IllegalArgumentException =>
        Rx.exception(e)
      }

    eval(rx) shouldBe Seq(OnNext(1), OnError(ex))
  }

  test("Rx.exception run") {
    val ex = new IllegalArgumentException("test")
    eval(Rx.exception(ex)) shouldBe Seq(OnError(ex))
  }

  test("event sequences") {

    test("single") {
      eval(Rx.single(1)) shouldBe Seq(OnNext(1), OnCompletion)
    }
    test("sequence") {
      eval(Rx.sequence(1, 2)) shouldBe Seq(OnNext(1), OnNext(2), OnCompletion)
    }
    test("fromSeq") {
      eval(Rx.fromSeq(Seq(1, 2))) shouldBe Seq(OnNext(1), OnNext(2), OnCompletion)
    }
    test("map") {
      eval(Rx.sequence(1, 2).map(_ * 2)) shouldBe Seq(OnNext(2), OnNext(4), OnCompletion)
    }

    test("map with exception") {
      val ex = new IllegalArgumentException("test")
      eval(
        Rx.sequence(1, 2, 3)
          .map { x =>
            x match
              case 1 =>
                1
              case 2 =>
                throw ex
              case 3 =>
                3
          }
      ) shouldBe Seq(OnNext(1), OnError(ex))
    }

    test("flatMap") {
      eval(Rx.sequence(1, 2, 3).flatMap(x => Rx.fromSeq((0 until x).map(_ => x)))) shouldBe
        Seq(OnNext(1), OnNext(2), OnNext(2), OnNext(3), OnNext(3), OnNext(3), OnCompletion)
    }

    test("flatMap with an error") {
      val ex = new IllegalArgumentException("test")
      eval(
        Rx.sequence(1, 2, 3)
          .flatMap {
            case 1 =>
              Rx.single("a")
            case 2 =>
              Rx.exception(ex)
            case _ =>
              Rx.single("b")
          }
      ) shouldBe Seq(OnNext("a"), OnError(ex))
    }

    test("filter") {
      eval(Rx.sequence(1, 2, 3).filter(_ % 2 == 1)) shouldBe
        Seq(OnNext(1), OnCompletion, OnNext(3), OnCompletion)
    }

    test("filter with error") {
      val ex = new IllegalArgumentException("test")
      eval(
        Rx.sequence(1, 2, 3)
          .filter(x =>
            if x == 2 then
              throw ex
            else
              x % 2 == 1
          )
      ) shouldBe Seq(OnNext(1), OnError(ex))
    }

    test("concat") {
      eval(Rx.sequence(1, 2).concat(Rx.sequence(3))) shouldBe
        Seq(OnNext(1), OnNext(2), OnNext(3), OnCompletion)
    }
  }

  test("continuous stream") {
    test("map") {
      val ex = new IllegalArgumentException(s"3")

      val v = Rx.variable(1)
      val rx = v.map { x =>
        if x == 3 then
          throw ex
        else
          x * 2
      }

      val b = Seq.newBuilder[RxEvent]
      val c = RxRunner.runContinuously(rx)(b += _)
      v := 2
      v := 3
      v := 4
      v := 5

      val events = b.result()
      debug(events)
      events shouldBe
        Seq(
          OnNext(2),
          OnNext(4),
          OnError(ex),
          OnNext(8), // Should keep reading the next update
          OnNext(10)
        )

      c.cancel
    }

    test("flatMap") {
      val ex = new IllegalArgumentException(s"3")

      val v = Rx.variable(1)
      val rx = v.flatMap { x =>
        if x == 3 then
          throw ex
        else
          Rx.single(x * 2)
      }

      val b = Seq.newBuilder[RxEvent]
      val c = RxRunner.runContinuously(rx)(b += _)
      v := 2
      v := 3
      v := 4
      v := 5

      val events = b.result()
      debug(events)
      events shouldBe
        Seq(
          OnNext(2),
          OnNext(4),
          OnError(ex),
          OnNext(8), // Should keep reading the next update
          OnNext(10)
        )

      c.cancel
    }

    test("filter") {
      val ex = new IllegalArgumentException("test")
      val v  = Rx.variable(1)
      val rx = v.filter(x =>
        if x == 2 then
          throw ex
        else
          x % 2 == 1
      )

      val b = Seq.newBuilder[RxEvent]
      RxRunner.runContinuously(rx)(b += _)

      (1 to 5).foreach(v := _)

      val events = b.result()
      debug(events)
      events shouldBe Seq(OnNext(1), OnError(ex), OnNext(3), OnCompletion, OnNext(5))
    }

    test("zip") {
      val ex = new IllegalArgumentException("test")
      val x  = Rx.variable(1)
      val y  = Rx.variable("a")
      val rx = x.zip(y)
      val b  = Seq.newBuilder[RxEvent]
      RxRunner.runContinuously(rx)(b += _)

      x := 2
      y := "b"
      y := "c"
      x := 3
      val events = b.result()
      debug(events)
      events shouldBe Seq(OnNext((1, "a")), OnNext((2, "b")), OnNext((3, "c")))
    }
  }

  test("rx.run without argument") {
    Rx.single(1).run()
    Rx.single(1).runContinuously()
    Rx.single(1).subscribe()
  }

end RxTest
