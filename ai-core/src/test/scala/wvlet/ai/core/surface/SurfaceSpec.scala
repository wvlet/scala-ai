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

package wvlet.ai.core.surface

import scala.language.implicitConversions
import wvlet.airspec.AirSpec

trait SurfaceSpec extends AirSpec:
  protected def check(body: => Surface, expectedName: String): Surface =
    val surface = body
    debug(s"[${surface.getClass.getSimpleName}] $surface, ${surface.fullName}")
    surface.toString shouldBe expectedName
    surface

  protected def checkPrimitive(body: => Surface, expectedName: String): Surface =
    val s = check(body, expectedName)
    assert(s.isAlias == false)
    assert(s.isOption == false)
    assert(s.isPrimitive == true)
    assert(s.objectFactory.isEmpty == true)
    s
