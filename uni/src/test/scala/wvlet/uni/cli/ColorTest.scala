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

class ColorTest extends UniTest:

  test("parse hex colors") {
    Color.fromHex("#FF5733") shouldBe Color.Rgb(255, 87, 51)
    Color.fromHex("FF5733") shouldBe Color.Rgb(255, 87, 51)
    Color.fromHex("#000000") shouldBe Color.Rgb(0, 0, 0)
    Color.fromHex("#FFFFFF") shouldBe Color.Rgb(255, 255, 255)
  }

  test("reject invalid hex colors") {
    intercept[IllegalArgumentException] {
      Color.fromHex("#FFF")
    }
    intercept[IllegalArgumentException] {
      Color.fromHex("invalid")
    }
  }

  test("generate foreground ANSI codes for standard colors") {
    Color.foregroundCode(Color.Black) shouldBe "\u001b[30m"
    Color.foregroundCode(Color.Red) shouldBe "\u001b[31m"
    Color.foregroundCode(Color.Green) shouldBe "\u001b[32m"
    Color.foregroundCode(Color.Yellow) shouldBe "\u001b[33m"
    Color.foregroundCode(Color.Blue) shouldBe "\u001b[34m"
    Color.foregroundCode(Color.Magenta) shouldBe "\u001b[35m"
    Color.foregroundCode(Color.Cyan) shouldBe "\u001b[36m"
    Color.foregroundCode(Color.White) shouldBe "\u001b[37m"
  }

  test("generate foreground ANSI codes for bright colors") {
    Color.foregroundCode(Color.BrightBlack) shouldBe "\u001b[90m"
    Color.foregroundCode(Color.BrightRed) shouldBe "\u001b[91m"
    Color.foregroundCode(Color.BrightGreen) shouldBe "\u001b[92m"
  }

  test("generate background ANSI codes for standard colors") {
    Color.backgroundCode(Color.Black) shouldBe "\u001b[40m"
    Color.backgroundCode(Color.Red) shouldBe "\u001b[41m"
    Color.backgroundCode(Color.Green) shouldBe "\u001b[42m"
  }

  test("generate ANSI codes for 256 colors") {
    Color.foregroundCode(Color.Ansi256(208)) shouldBe "\u001b[38;5;208m"
    Color.backgroundCode(Color.Ansi256(208)) shouldBe "\u001b[48;5;208m"
  }

  test("generate ANSI codes for RGB colors") {
    Color.foregroundCode(Color.Rgb(255, 87, 51)) shouldBe "\u001b[38;2;255;87;51m"
    Color.backgroundCode(Color.Rgb(255, 87, 51)) shouldBe "\u001b[48;2;255;87;51m"
  }

  test("Gray is alias for BrightBlack") {
    Color.Gray shouldBe Color.BrightBlack
  }

end ColorTest
