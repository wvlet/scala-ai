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
import ChalkOps.*

class ChalkOpsTest extends UniTest:

  test("string extension for foreground colors") {
    "hello".red shouldBe Chalk.red("hello")
    "hello".green shouldBe Chalk.green("hello")
    "hello".blue shouldBe Chalk.blue("hello")
    "hello".yellow shouldBe Chalk.yellow("hello")
    "hello".cyan shouldBe Chalk.cyan("hello")
    "hello".magenta shouldBe Chalk.magenta("hello")
    "hello".white shouldBe Chalk.white("hello")
    "hello".black shouldBe Chalk.black("hello")
    "hello".gray shouldBe Chalk.gray("hello")
  }

  test("string extension for bright colors") {
    "hello".brightRed shouldBe Chalk.brightRed("hello")
    "hello".brightGreen shouldBe Chalk.brightGreen("hello")
    "hello".brightBlue shouldBe Chalk.brightBlue("hello")
  }

  test("string extension for modifiers") {
    "hello".bold shouldBe Chalk.bold("hello")
    "hello".dim shouldBe Chalk.dim("hello")
    "hello".italic shouldBe Chalk.italic("hello")
    "hello".underline shouldBe Chalk.underline("hello")
    "hello".strikethrough shouldBe Chalk.strikethrough("hello")
    "hello".inverse shouldBe Chalk.inverse("hello")
  }

  test("string extension with custom chalk style") {
    val errorStyle = Chalk.red.bold
    "Error!".chalk(errorStyle) shouldBe errorStyle("Error!")
  }

end ChalkOpsTest
