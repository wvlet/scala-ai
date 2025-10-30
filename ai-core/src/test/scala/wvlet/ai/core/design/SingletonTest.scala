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
package wvlet.ai.core.design

import wvlet.ai.core.surface.Surface
import wvlet.airspec.AirSpec
import wvlet.ai.core.log.LogLevel
import wvlet.ai.core.log.LogSupport
import wvlet.ai.core.log.Logger

import java.util.concurrent.atomic.AtomicInteger

object SingletonTest:
  type TraitCounter = AtomicInteger

  // This doesn't tell about Singleton
  class X(val counter: TraitCounter) extends LogSupport:
    debug("new X is instantiated")

  class A(val t: X)
  class B(val t: X)
  class SingletonService(val service: X)

  class U1(service: X) extends SingletonService(service)
  class U2(service: X) extends SingletonService(service)

  trait NonAbstract extends LogSupport:
    def hello: String = "hello"

  class C() extends NonAbstract:
    override def hello = "nice"

  class E(val m: NonAbstract) extends LogSupport

/**
  */
class SingletonTest extends AirSpec:
  import SingletonTest.*

  def d = Design
    .newDesign
    .bindInstance[TraitCounter](new AtomicInteger(0))
    .onInit { c =>
      val v = c.incrementAndGet()
      debug(s"Counter is initialized: ${v}")
    }

  test("bind singleton with bind[X]") {
    val session = d.newSession

    val a = session.build[A]
    val b = session.build[B]

    a.t.counter shouldBeTheSameInstanceAs b.t.counter
    session.build[TraitCounter].get() shouldBe 1
  }

  test("bind singleton with bind[X] as a service") {
    val session = d.newSession

    val u1 = session.build[U1]
    val u2 = session.build[U2]

    u1.service.counter shouldBeTheSameInstanceAs u2.service.counter
    u1.service.counter.get() shouldBe 1
  }

  test("support overriding non-abstract singleton trait") {
    val d = Design.newDesign.bindSingleton[E].bindImpl[NonAbstract, C]

    info(Surface.of[C].getClass)
    val session = d.newSession
    val e       = session.build[E]
    e.m.hello shouldBe "nice"
  }

end SingletonTest
