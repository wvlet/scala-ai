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
package wvlet.uni.design.lifecycle

import wvlet.uni.design.Design
import wvlet.uni.test.UniTest
import wvlet.uni.test.empty
import wvlet.uni.test.defined

import java.util.concurrent.atomic.AtomicBoolean

/**
  */
object CloseableShutdownHookTest extends UniTest:
  class A extends AutoCloseable:
    val closeIsCalled = new AtomicBoolean(false)

    override def close(): Unit = closeIsCalled.set(true)

  test("support closeable") {
    val a = new A
    val d = Design.newSilentDesign.bindInstance[A](a)
    d.build[A] { a =>
    }

    a.closeIsCalled.get() shouldBe true
  }

  class A1(val a: A)

  test("favor onShutdownHook") {
    val onShutdownIsCalled = new AtomicBoolean(false)

    val x: A1 = Design
      .newSilentDesign
      .bindSingleton[A]
      .onShutdown { x =>
        onShutdownIsCalled.set(true)
      }
      .run { (a1: A1) =>
        a1
      }

    onShutdownIsCalled.get shouldBe true
    x.a.closeIsCalled.get shouldBe false
  }

end CloseableShutdownHookTest
