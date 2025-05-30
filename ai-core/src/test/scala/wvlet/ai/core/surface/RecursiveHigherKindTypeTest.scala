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

import scala.language.higherKinds

object RecursiveHigherKindTypeTest:
  trait Holder[M[*]]

  class MyTask[A]

  object Holder:
    type BySkinny[A] = MyTask[A]
    def bySkinny: Holder[BySkinny] = new InterpretedHolder

  import Holder.*
  class InterpretedHolder extends Holder[BySkinny] {}

/**
  */
class RecursiveHigherKindTypeTest extends SurfaceSpec:

  import RecursiveHigherKindTypeTest.*
  import Holder.BySkinny

  test("support recursive higher kind types") {
    val s = Surface.of[Holder[BySkinny]]
    s.name shouldBe "Holder[BySkinny]"
    s.isAlias shouldBe false
    s.isPrimitive shouldBe false
    s.isOption shouldBe false
    s.dealias.toString shouldBe "Holder[BySkinny]"

    s.typeArgs(0).name shouldBe "BySkinny"
    s.typeArgs(0).dealias.name shouldBe "MyTask[A]"
  }
