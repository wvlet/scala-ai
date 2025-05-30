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
package wvlet.ai.core.design.lifecycle

import wvlet.ai.core.design.Design
import wvlet.airspec.AirSpec

import java.util.concurrent.atomic.AtomicInteger

/**
  */
class DesignTimeLifeCycleHookTest extends AirSpec:
  test("support design time bindings") {
    val order              = new AtomicInteger(1)
    val initializedTime    = new AtomicInteger(0)
    val injectTime         = new AtomicInteger(0)
    val startTime          = new AtomicInteger(0)
    val afterStartTime     = new AtomicInteger(0)
    val beforeShutdownTime = new AtomicInteger(0)
    val shutdownTime       = new AtomicInteger(0)

    val d = Design
      .newSilentDesign
      .bindInstance[String]("hello")
      .onInit(x => initializedTime.set(order.getAndIncrement()))
      .onInject(x => injectTime.set(order.getAndIncrement()))
      .onStart(x => startTime.set(order.getAndIncrement()))
      .afterStart(x => afterStartTime.set(order.getAndIncrement()))
      .beforeShutdown(x => beforeShutdownTime.set(order.getAndIncrement()))
      .onShutdown(x => shutdownTime.set(order.getAndIncrement()))

    d.build[String] { s =>
      //
    }

    initializedTime.get shouldBe 1
    injectTime.get shouldBe 2
    startTime.get shouldBe 3
    afterStartTime.get shouldBe 4
    beforeShutdownTime.get shouldBe 5
    shutdownTime.get shouldBe 6
  }

  test("add lifecycle only") {
    val v = new AtomicInteger(0)
    val d = Design.newSilentDesign.bindInstance[AtomicInteger](v)

    val d2 = d
      .onStart { x =>
        x.addAndGet(1)
      }
      .afterStart { x =>
        x.addAndGet(1 << 1)
      }
      .onShutdown { x =>
        x.addAndGet(1 << 2)
      }
      .beforeShutdown { x =>
        x.addAndGet(1 << 3)
      }
      .onInit { x =>
        x.addAndGet(1 << 4)
      }
      .onInject { x =>
        x.addAndGet(1 << 5)
      }

    d2.withSession { s =>
    }

    v.get() shouldBe 0x3f
  }

end DesignTimeLifeCycleHookTest
