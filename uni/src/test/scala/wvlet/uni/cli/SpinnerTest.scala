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

class SpinnerTest extends UniTest:

  test("create spinner with default settings") {
    val spinner = Spinner()
    spinner.text shouldBe ""
    spinner.style shouldBe SpinnerStyle.Default
    spinner.enabled shouldBe true
    spinner.hideCursor shouldBe true
  }

  test("build spinner with custom settings") {
    val spinner = Spinner()
      .withText("Loading...")
      .withSpinner(SpinnerStyle.Line)
      .withColor(Chalk.magenta)
      .withEnabled(false)
      .withHideCursor(false)

    spinner.text shouldBe "Loading..."
    spinner.style shouldBe SpinnerStyle.Line
    spinner.enabled shouldBe false
    spinner.hideCursor shouldBe false
  }

  test("noSpinner disables spinner") {
    val spinner = Spinner().noSpinner
    spinner.enabled shouldBe false
  }

  test("noColor disables color output") {
    val spinner = Spinner().noColor
    spinner.color.enabled shouldBe false
  }

  test("start with text override") {
    // This test just verifies the API works; actual animation is not tested
    val spinner = Spinner().withText("Original").withEnabled(false)
    val running = spinner.start("Override")
    running.text shouldBe "Override"
    running.stop()
  }

  test("disabled spinner can start and stop") {
    val spinner = Spinner().noSpinner.start("Test")
    spinner.isSpinning shouldBe true
    spinner.stop()
    spinner.isSpinning shouldBe false
  }

  test("disabled spinner succeed works") {
    val spinner = Spinner().noSpinner.start("Test")
    spinner.succeed("Done")
    spinner.isSpinning shouldBe false
  }

  test("disabled spinner fail works") {
    val spinner = Spinner().noSpinner.start("Test")
    spinner.fail("Error")
    spinner.isSpinning shouldBe false
  }

end SpinnerTest
