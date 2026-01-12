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

class ProgressBarTest extends AirSpec:

  test("create progress bar with required total") {
    val bar = ProgressBar(total = 100)
    bar.total shouldBe 100
    bar.width shouldBe 40
    bar.complete shouldBe "█"
    bar.incomplete shouldBe "░"
    bar.showPercent shouldBe true
    bar.showCount shouldBe false
    bar.enabled shouldBe true
  }

  test("reject zero or negative total") {
    intercept[IllegalArgumentException] {
      ProgressBar(total = 0)
    }
    intercept[IllegalArgumentException] {
      ProgressBar(total = -1)
    }
  }

  test("reject zero or negative width") {
    intercept[IllegalArgumentException] {
      ProgressBar(total = 100, width = 0)
    }
  }

  test("build progress bar with custom settings") {
    val bar = ProgressBar(total = 100)
      .withWidth(30)
      .withComplete("=")
      .withIncomplete("-")
      .withPrefix("Progress: ")
      .withSuffix(" items")
      .withShowPercent(false)
      .withShowCount(true)
      .withColor(Chalk.cyan)
      .withEnabled(false)
      .withHideCursor(false)

    bar.width shouldBe 30
    bar.complete shouldBe "="
    bar.incomplete shouldBe "-"
    bar.prefix shouldBe "Progress: "
    bar.suffix shouldBe " items"
    bar.showPercent shouldBe false
    bar.showCount shouldBe true
    bar.enabled shouldBe false
    bar.hideCursor shouldBe false
  }

  test("noProgressBar disables progress bar") {
    val bar = ProgressBar(total = 100).noProgressBar
    bar.enabled shouldBe false
  }

  test("noColor disables color output") {
    val bar = ProgressBar(total = 100).noColor
    bar.color.enabled shouldBe false
  }

  test("disabled progress bar tracks progress") {
    val bar = ProgressBar(total = 100).noProgressBar.start()
    bar.current shouldBe 0
    bar.isComplete shouldBe false

    bar.update(50)
    bar.current shouldBe 50
    bar.isComplete shouldBe false

    bar.update(100)
    bar.current shouldBe 100
    bar.isComplete shouldBe true

    bar.finish()
  }

  test("disabled progress bar increment works") {
    val bar = ProgressBar(total = 10).noProgressBar.start()
    bar.current shouldBe 0

    bar.increment()
    bar.current shouldBe 1

    bar.increment(5)
    bar.current shouldBe 6

    bar.increment(10) // Should cap at total
    bar.current shouldBe 10
    bar.isComplete shouldBe true

    bar.finish()
  }

  test("disabled progress bar does not exceed total") {
    val bar = ProgressBar(total = 10).noProgressBar.start()
    bar.update(20) // Should cap at 10
    bar.current shouldBe 10
    bar.finish()
  }

end ProgressBarTest
