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

class ChalkTest extends AirSpec:
  private val RESET = "\u001b[0m"

  test("apply foreground color") {
    Chalk.red("hello") shouldBe s"\u001b[31mhello${RESET}"
    Chalk.green("hello") shouldBe s"\u001b[32mhello${RESET}"
    Chalk.blue("hello") shouldBe s"\u001b[34mhello${RESET}"
    Chalk.yellow("hello") shouldBe s"\u001b[33mhello${RESET}"
    Chalk.cyan("hello") shouldBe s"\u001b[36mhello${RESET}"
    Chalk.magenta("hello") shouldBe s"\u001b[35mhello${RESET}"
    Chalk.white("hello") shouldBe s"\u001b[37mhello${RESET}"
    Chalk.black("hello") shouldBe s"\u001b[30mhello${RESET}"
  }

  test("apply bright foreground colors") {
    Chalk.brightRed("hello") shouldBe s"\u001b[91mhello${RESET}"
    Chalk.brightGreen("hello") shouldBe s"\u001b[92mhello${RESET}"
    Chalk.gray("hello") shouldBe s"\u001b[90mhello${RESET}"
  }

  test("apply text modifiers") {
    Chalk.bold("hello") shouldBe s"\u001b[1mhello${RESET}"
    Chalk.dim("hello") shouldBe s"\u001b[2mhello${RESET}"
    Chalk.italic("hello") shouldBe s"\u001b[3mhello${RESET}"
    Chalk.underline("hello") shouldBe s"\u001b[4mhello${RESET}"
    Chalk.strikethrough("hello") shouldBe s"\u001b[9mhello${RESET}"
    Chalk.inverse("hello") shouldBe s"\u001b[7mhello${RESET}"
  }

  test("chain foreground color and modifier") {
    val result = Chalk.red.bold("hello")
    result shouldContain "\u001b[31m"
    result shouldContain "\u001b[1m"
    result shouldContain "hello"
    result shouldContain RESET
  }

  test("chain multiple modifiers") {
    val result = Chalk.red.bold.underline("hello")
    result shouldContain "\u001b[31m"
    result shouldContain "\u001b[1m"
    result shouldContain "\u001b[4m"
    result shouldContain "hello"
    result shouldContain RESET
  }

  test("apply background color") {
    val result = Chalk().bgRed("hello")
    result shouldContain "\u001b[41m"
    result shouldContain "hello"
    result shouldContain RESET
  }

  test("combine foreground and background colors") {
    val result = Chalk.white.bgBlue("hello")
    result shouldContain "\u001b[37m"
    result shouldContain "\u001b[44m"
    result shouldContain "hello"
    result shouldContain RESET
  }

  test("disabled chalk returns plain text") {
    Chalk().noColor("hello") shouldBe "hello"
    Chalk.red.noColor("hello") shouldBe "hello"
  }

  test("return plain text when no styling applied") {
    Chalk()("hello") shouldBe "hello"
  }

  test("support RGB colors") {
    val result = Chalk().rgb(255, 87, 51)("hello")
    result shouldContain "\u001b[38;2;255;87;51m"
    result shouldContain "hello"
    result shouldContain RESET
  }

  test("support hex colors") {
    val result = Chalk().hex("#FF5733")("hello")
    result shouldContain "\u001b[38;2;255;87;51m"
    result shouldContain "hello"
    result shouldContain RESET
  }

  test("support 256 colors") {
    val result = Chalk().ansi256(208)("hello")
    result shouldContain "\u001b[38;5;208m"
    result shouldContain "hello"
    result shouldContain RESET
  }

  test("support background RGB colors") {
    val result = Chalk().bgRgb(255, 87, 51)("hello")
    result shouldContain "\u001b[48;2;255;87;51m"
    result shouldContain "hello"
    result shouldContain RESET
  }

  test("create reusable styles") {
    val errorStyle   = Chalk.red.bold
    val successStyle = Chalk.green

    errorStyle("Failed!") shouldContain "\u001b[31m"
    errorStyle("Failed!") shouldContain "\u001b[1m"
    successStyle("Passed!") shouldContain "\u001b[32m"
  }

  test("reset returns empty chalk") {
    val styled = Chalk.red.bold
    val reset  = styled.reset
    reset("hello") shouldBe "hello"
  }

end ChalkTest
