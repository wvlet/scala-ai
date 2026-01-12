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
package wvlet.uni.cli

import wvlet.airspec.AirSpec

class SpinnerStyleTest extends AirSpec:

  test("Dots style has correct frames") {
    SpinnerStyle.Dots.frames.size shouldBe 10
    SpinnerStyle.Dots.interval shouldBe 80
    SpinnerStyle.Dots.frames.head shouldBe "⠋"
  }

  test("Line style has correct frames") {
    SpinnerStyle.Line.frames.size shouldBe 4
    SpinnerStyle.Line.interval shouldBe 130
    SpinnerStyle.Line.frames shouldBe Seq("-", "\\", "|", "/")
  }

  test("Arrow style has correct frames") {
    SpinnerStyle.Arrow.frames.size shouldBe 8
    SpinnerStyle.Arrow.interval shouldBe 120
  }

  test("Circle style has correct frames") {
    SpinnerStyle.Circle.frames.size shouldBe 4
    SpinnerStyle.Circle.interval shouldBe 100
  }

  test("SimpleDots style has correct frames") {
    SpinnerStyle.SimpleDots.frames.size shouldBe 3
    SpinnerStyle.SimpleDots.interval shouldBe 300
    SpinnerStyle.SimpleDots.frames shouldBe Seq(".", "..", "...")
  }

  test("Custom style can be created") {
    val custom = SpinnerStyle.Custom(Seq("A", "B", "C"), 200)
    custom.frames shouldBe Seq("A", "B", "C")
    custom.interval shouldBe 200
  }

  test("Default style is Dots") {
    SpinnerStyle.Default shouldBe SpinnerStyle.Dots
  }

end SpinnerStyleTest
