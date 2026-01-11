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
package wvlet.uni.core.design

import DesignErrorCode.MISSING_DEPENDENCY
import wvlet.unirspec.AirSpec

object DisableNoDefaultInstanceCreationTest:
  case class Component(config: Config)
  case class Config(value: String = "test")

class DisableNoDefaultInstanceCreationTest extends AirSpec:
  import DisableNoDefaultInstanceCreationTest.*

  test("disable implicit instance creation") {
    val d = Design.newSilentDesign.bindSingleton[Component].noDefaultInstanceInjection
    val e = intercept[DesignException] {
      d.build[Component] { _ =>
      }
    }
    e.code shouldBe MISSING_DEPENDENCY
  }

  test("disable implicit instance creation with production mode") {
    val d =
      Design.newSilentDesign.bindSingleton[Component].noDefaultInstanceInjection.withProductionMode
    val e = intercept[DesignException] {
      d.withSession { _ =>
      }
    }
    e.code shouldBe MISSING_DEPENDENCY
  }

  test("enable implicit instance creation") {
    val d = Design.newSilentDesign.bindSingleton[Component]
    d.build[Component] { c =>
      assert(c.config.value == "test")
    }
  }

end DisableNoDefaultInstanceCreationTest
