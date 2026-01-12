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
package wvlet.uni.design

import wvlet.airspec.AirSpec
import wvlet.uni.log.LogSupport

object ProviderExample extends Serializable:
  case class D1(id: Int)
  case class D2(id: Int)
  case class D3(id: Int)
  case class D4(id: Int)
  case class D5(id: Int)
  case class App(d1: D1 = D1(0), d2: D2 = D2(0), d3: D3 = D3(0), d4: D4 = D4(0), d5: D5 = D5(0))
      extends LogSupport:
    debug(s"Created ${toString()}")

  val d1 = D1(1)
  val d2 = D2(2)
  val d3 = D3(3)
  val d4 = D4(4)
  val d5 = D5(5)

  val z1 = D1(0)
  val z2 = D2(0)
  val z3 = D3(0)
  val z4 = D4(0)
  val z5 = D5(0)

  val providerDesign = Design
    .newDesign
    .bindInstance[D1](d1)
    .bindInstance[D2](d2)
    .bindInstance[D3](d3)
    .bindInstance[D4](d4)
    .bindInstance[D5](d5)

  def provider1(d1: D1): App                                 = App(d1)
  def provider2(d1: D1, d2: D2): App                         = App(d1, d2)
  def provider3(d1: D1, d2: D2, d3: D3): App                 = App(d1, d2, d3)
  def provider4(d1: D1, d2: D2, d3: D3, d4: D4): App         = App(d1, d2, d3, d4)
  def provider5(d1: D1, d2: D2, d3: D3, d4: D4, d5: D5): App = App(d1, d2, d3, d4, d5)

end ProviderExample

import ProviderExample.*

class SingletonProviderTest extends AirSpec:
  test("build singleton from provider bindings") {
    val s1 =
      providerDesign
        .bindProvider { (d1: D1) =>
          App(d1)
        }
        .newSession
    val p1 = s1.build[App]
    p1 shouldBe App(d1, z2, z3, z4, z5)
    p1 shouldBeTheSameInstanceAs s1.build[App]

    val s2 =
      providerDesign
        .bindProvider { (d1: D1, d2: D2) =>
          App(d1, d2)
        }
        .newSession
    val p2 = s2.build[App]
    p2 shouldBe App(d1, d2, z3, z4, z5)
    p2 shouldBeTheSameInstanceAs s2.build[App]

    val s3 =
      providerDesign
        .bindProvider { (d1: D1, d2: D2, d3: D3) =>
          App(d1, d2, d3)
        }
        .newSession
    val p3 = s3.build[App]
    p3 shouldBe App(d1, d2, d3, z4, z5)
    p3 shouldBeTheSameInstanceAs s3.build[App]

    val s4 =
      providerDesign
        .bindProvider { (d1: D1, d2: D2, d3: D3, d4: D4) =>
          App(d1, d2, d3, d4)
        }
        .newSession
    val p4 = s4.build[App]
    p4 shouldBe App(d1, d2, d3, d4, z5)
    p4 shouldBeTheSameInstanceAs s4.build[App]

    val s5 =
      providerDesign
        .bindProvider { (d1: D1, d2: D2, d3: D3, d4: D4, d5: D5) =>
          App(d1, d2, d3, d4, d5)
        }
        .newSession
    val p5 = s5.build[App]
    p5 shouldBe App(d1, d2, d3, d4, d5)
    p5 shouldBeTheSameInstanceAs s5.build[App]
  }

end SingletonProviderTest
