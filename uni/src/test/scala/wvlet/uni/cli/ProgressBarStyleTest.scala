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

import wvlet.uni.test.UniTest
import wvlet.uni.test.empty
import wvlet.uni.test.defined

class ProgressBarStyleTest extends UniTest:

  test("Default style uses solid blocks") {
    val bar = ProgressBarStyle.Default(100)
    bar.complete shouldBe "█"
    bar.incomplete shouldBe "░"
  }

  test("Shaded style uses gradient blocks") {
    val bar = ProgressBarStyle.Shaded(100)
    bar.complete shouldBe "▓"
    bar.incomplete shouldBe "░"
  }

  test("Block style uses filled/empty squares") {
    val bar = ProgressBarStyle.Block(100)
    bar.complete shouldBe "■"
    bar.incomplete shouldBe "□"
  }

  test("Arrow style is ASCII-compatible") {
    val bar = ProgressBarStyle.Arrow(100)
    bar.complete shouldBe "="
    bar.incomplete shouldBe "-"
  }

  test("Hash style is ASCII-compatible") {
    val bar = ProgressBarStyle.Hash(100)
    bar.complete shouldBe "#"
    bar.incomplete shouldBe "."
  }

  test("Classic style has percentage and count") {
    val bar = ProgressBarStyle.Classic(100, "Downloading: ")
    bar.showPercent shouldBe true
    bar.showCount shouldBe true
    bar.prefix shouldBe "Downloading: "
    bar.width shouldBe 30
  }

  test("Minimal style has no percentage") {
    val bar = ProgressBarStyle.Minimal(100)
    bar.showPercent shouldBe false
  }

  test("All styles can start and finish") {
    val styles = Seq(
      ProgressBarStyle.Default(10),
      ProgressBarStyle.Shaded(10),
      ProgressBarStyle.Block(10),
      ProgressBarStyle.Arrow(10),
      ProgressBarStyle.Hash(10),
      ProgressBarStyle.Classic(10),
      ProgressBarStyle.Minimal(10),
      ProgressBarStyle.Rainbow(10)
    )

    styles.foreach { style =>
      val bar = style.noProgressBar.start()
      bar.increment(5)
      bar.current shouldBe 5
      bar.finish()
    }
  }

end ProgressBarStyleTest
