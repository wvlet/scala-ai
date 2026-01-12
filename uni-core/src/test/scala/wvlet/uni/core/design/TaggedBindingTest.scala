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

import wvlet.uni.core.surface.Surface
import wvlet.uni.core.surface.tag.*
import wvlet.airspec.AirSpec

/**
  */
object TaggedBindingTest extends AirSpec:
  case class Fruit(name: String)

  trait Apple
  trait Banana
  trait Lemon

  class TaggedBinding(
      val apple: Fruit @@ Apple,
      val banana: Fruit @@ Banana,
      val lemon: Fruit @@ Lemon
  )

  test("support tagged binding") {
    val apple = Surface.of[Fruit @@ Apple]
    debug(s"apple: ${apple}, alias:${apple.isAlias}")

    val d = Design
      .newDesign
      .bindInstance[Fruit @@ Apple](Fruit("apple"))
      .bindInstance[Fruit @@ Banana](Fruit("banana"))
      .bindProvider { (apple: Fruit @@ Apple) =>
        Fruit(s"lemon+${apple.name}").asInstanceOf[Fruit @@ Lemon]
      }

    pending("Tagged type within constructor doesn't work in Scala.js")
//    d.build[TaggedBinding] { tagged: TaggedBinding =>
//      tagged.apple.name shouldBe "apple"
//      tagged.banana.name shouldBe "banana"
//      tagged.lemon.name shouldBe "lemon+apple"
//    }
  }

end TaggedBindingTest
