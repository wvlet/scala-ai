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
import TintOps.*

class TintOpsTest extends UniTest:

  test("string extension for foreground colors") {
    "hello".red shouldBe Tint.red("hello")
    "hello".green shouldBe Tint.green("hello")
    "hello".blue shouldBe Tint.blue("hello")
    "hello".yellow shouldBe Tint.yellow("hello")
    "hello".cyan shouldBe Tint.cyan("hello")
    "hello".magenta shouldBe Tint.magenta("hello")
    "hello".white shouldBe Tint.white("hello")
    "hello".black shouldBe Tint.black("hello")
    "hello".gray shouldBe Tint.gray("hello")
  }

  test("string extension for bright colors") {
    "hello".brightRed shouldBe Tint.brightRed("hello")
    "hello".brightGreen shouldBe Tint.brightGreen("hello")
    "hello".brightBlue shouldBe Tint.brightBlue("hello")
  }

  test("string extension for modifiers") {
    "hello".bold shouldBe Tint.bold("hello")
    "hello".dim shouldBe Tint.dim("hello")
    "hello".italic shouldBe Tint.italic("hello")
    "hello".underline shouldBe Tint.underline("hello")
    "hello".strikethrough shouldBe Tint.strikethrough("hello")
    "hello".inverse shouldBe Tint.inverse("hello")
  }

  test("string extension with custom tint style") {
    val errorStyle = Tint.red.bold
    "Error!".tint(errorStyle) shouldBe errorStyle("Error!")
  }

end TintOpsTest
