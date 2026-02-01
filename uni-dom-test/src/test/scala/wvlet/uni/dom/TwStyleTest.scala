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
package wvlet.uni.dom

import wvlet.uni.test.UniTest
import wvlet.uni.dom.all.*
import wvlet.uni.dom.all.given

class TwStyleTest extends UniTest:

  test("create TwStyle from string"):
    val s = tw("flex items-center")
    s.classes shouldBe "flex items-center"

  test("create TwStyle from multiple strings"):
    val s = tw("flex", "items-center", "gap-4")
    s.classes shouldBe "flex items-center gap-4"

  test("combine TwStyles with + operator"):
    val base    = tw("px-4 py-2")
    val colors  = tw("bg-blue-500 text-white")
    val combined = base + colors
    combined.classes shouldBe "px-4 py-2 bg-blue-500 text-white"

  test("combine TwStyles with ++ operator"):
    val base    = tw("px-4")
    val layout  = tw("flex")
    val colors  = tw("bg-blue-500")
    val combined = base ++ (layout, colors)
    combined.classes shouldBe "px-4 flex bg-blue-500"

  test("add hover variant"):
    val s = tw("bg-blue-500").hover("bg-blue-600")
    s.classes shouldBe "bg-blue-500 hover:bg-blue-600"

  test("add multiple hover classes"):
    val s = tw("bg-blue-500").hover("bg-blue-600 scale-105")
    s.classes shouldBe "bg-blue-500 hover:bg-blue-600 hover:scale-105"

  test("add focus variant"):
    val s = tw("outline-none").focus("ring-2 ring-blue-500")
    s.classes shouldBe "outline-none focus:ring-2 focus:ring-blue-500"

  test("add active variant"):
    val s = tw("bg-blue-500").active("bg-blue-700")
    s.classes shouldBe "bg-blue-500 active:bg-blue-700"

  test("add disabled variant"):
    val s = tw("bg-blue-500").disabled("opacity-50 cursor-not-allowed")
    s.classes shouldBe "bg-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"

  test("add dark mode variant"):
    val s = tw("bg-white text-gray-900").dark("bg-gray-900 text-white")
    s.classes shouldBe "bg-white text-gray-900 dark:bg-gray-900 dark:text-white"

  test("add responsive sm variant"):
    val s = tw("text-sm").sm("text-base")
    s.classes shouldBe "text-sm sm:text-base"

  test("add responsive md variant"):
    val s = tw("hidden").md("block")
    s.classes shouldBe "hidden md:block"

  test("add responsive lg variant"):
    val s = tw("text-base").lg("text-lg")
    s.classes shouldBe "text-base lg:text-lg"

  test("add responsive xl variant"):
    val s = tw("max-w-md").xl("max-w-lg")
    s.classes shouldBe "max-w-md xl:max-w-lg"

  test("add responsive 2xl variant"):
    val s = tw("max-w-lg").xxl("max-w-xl")
    s.classes shouldBe "max-w-lg 2xl:max-w-xl"

  test("chain multiple variants"):
    val s = tw("bg-white")
      .hover("bg-gray-100")
      .focus("ring-2")
      .dark("bg-gray-900")
      .md("p-4")
    s.classes shouldBe "bg-white hover:bg-gray-100 focus:ring-2 dark:bg-gray-900 md:p-4"

  test("add group-hover variant"):
    val s = tw("opacity-0").groupHover("opacity-100")
    s.classes shouldBe "opacity-0 group-hover:opacity-100"

  test("add focus-within variant"):
    val s = tw("border-gray-300").focusWithin("border-blue-500")
    s.classes shouldBe "border-gray-300 focus-within:border-blue-500"

  test("add focus-visible variant"):
    val s = tw("outline-none").focusVisible("ring-2")
    s.classes shouldBe "outline-none focus-visible:ring-2"

  test("add first/last variants"):
    val s = tw("border-t").first("border-t-0").last("border-b-0")
    s.classes shouldBe "border-t first:border-t-0 last:border-b-0"

  test("add odd/even variants"):
    val s = tw("bg-white").odd("bg-gray-50").even("bg-gray-100")
    s.classes shouldBe "bg-white odd:bg-gray-50 even:bg-gray-100"

  test("add custom variant"):
    val s = tw("text-gray-500").variant("placeholder", "text-gray-400")
    s.classes shouldBe "text-gray-500 placeholder:text-gray-400"

  test("TwStyle.when with true condition"):
    val s = TwStyle.when(true)("bg-blue-500")
    s.classes shouldBe "bg-blue-500"

  test("TwStyle.when with false condition"):
    val s = TwStyle.when(false)("bg-blue-500")
    s.classes shouldBe ""

  test("TwStyle.unless with true condition"):
    val s = TwStyle.unless(true)("bg-red-500")
    s.classes shouldBe ""

  test("TwStyle.unless with false condition"):
    val s = TwStyle.unless(false)("bg-red-500")
    s.classes shouldBe "bg-red-500"

  test("TwStyle.toggle"):
    val active   = TwStyle.toggle(true, "bg-blue-500", "bg-gray-500")
    val inactive = TwStyle.toggle(false, "bg-blue-500", "bg-gray-500")
    active.classes shouldBe "bg-blue-500"
    inactive.classes shouldBe "bg-gray-500"

  test("TwStyle.fromOption with Some"):
    val s = TwStyle.fromOption(Some("bg-blue-500"))
    s.classes shouldBe "bg-blue-500"

  test("TwStyle.fromOption with None"):
    val s = TwStyle.fromOption(None)
    s.classes shouldBe ""

  test("TwStyle.merge"):
    val merged = TwStyle.merge(
      Some(tw("flex")),
      None,
      Some(tw("items-center")),
      Some(tw("gap-4"))
    )
    merged.classes shouldBe "flex items-center gap-4"

  test("TwStyle.empty"):
    TwStyle.empty.isEmpty shouldBe true
    TwStyle.empty.nonEmpty shouldBe false

  test("isEmpty and nonEmpty"):
    tw("flex").isEmpty shouldBe false
    tw("flex").nonEmpty shouldBe true
    tw("").isEmpty shouldBe true
    tw("").nonEmpty shouldBe false

  test("combining empty with non-empty"):
    val s = TwStyle.empty + tw("flex") + TwStyle.empty
    s.classes shouldBe "flex"

  test("trimming whitespace"):
    val s = TwStyle("  flex   items-center  ")
    s.classes shouldBe "flex   items-center"

  test("use TwStyle in div element"):
    val d = div(tw("flex items-center"))
    d.modifiers.flatten.size shouldBe 1
    d.modifiers.flatten.head shouldMatch { case s: TwStyle =>
      s.classes shouldBe "flex items-center"
    }

  test("use combined TwStyle in element"):
    val buttonStyle = tw("px-4 py-2") + tw("bg-blue-500")
    val b           = button(buttonStyle, "Click me")
    b.modifiers.flatten.size shouldBe 2

  test("use multiple TwStyles in element"):
    val layout = tw("flex items-center")
    val colors = tw("bg-white text-gray-900")
    val d      = div(layout, colors)
    d.modifiers.flatten.size shouldBe 2

  test("mix TwStyle with regular attributes"):
    val d = div(
      tw("flex items-center"),
      id -> "container",
      tw("gap-4")
    )
    d.modifiers.flatten.size shouldBe 3

  // TwClasses builder tests

  test("TwClasses basic usage"):
    val classes = TwClasses()
      .add("flex items-center")
      .build()
    classes.classes shouldBe "flex items-center"

  test("TwClasses with conditional addIf"):
    val classes = TwClasses()
      .add("flex")
      .addIf(true, "items-center")
      .addIf(false, "items-start")
      .build()
    classes.classes shouldBe "flex items-center"

  test("TwClasses with conditional addUnless"):
    val classes = TwClasses()
      .add("flex")
      .addUnless(true, "hidden")
      .addUnless(false, "visible")
      .build()
    classes.classes shouldBe "flex visible"

  test("TwClasses with addOption"):
    val classes = TwClasses()
      .add("flex")
      .addOption(Some("items-center"))
      .addOption(None)
      .build()
    classes.classes shouldBe "flex items-center"

  test("TwClasses with addMatch"):
    val variant = "primary"
    val classes = TwClasses()
      .add("px-4 py-2")
      .addMatch(variant) {
        case "primary"   => "bg-blue-500 text-white"
        case "secondary" => "bg-gray-200 text-gray-800"
        case _           => "bg-white"
      }
      .build()
    classes.classes shouldBe "px-4 py-2 bg-blue-500 text-white"

  test("TwClasses with addMatch non-matching"):
    val variant = "unknown"
    val classes = TwClasses()
      .add("px-4 py-2")
      .addMatch(variant) {
        case "primary"   => "bg-blue-500"
        case "secondary" => "bg-gray-500"
      }
      .build()
    classes.classes shouldBe "px-4 py-2"

  test("TwClasses with addVariantIf"):
    val classes = TwClasses()
      .add("bg-blue-500")
      .addVariantIf(true, "hover", "bg-blue-600 scale-105")
      .addVariantIf(false, "focus", "ring-2")
      .build()
    classes.classes shouldBe "bg-blue-500 hover:bg-blue-600 hover:scale-105"

  test("TwClasses with TwStyle"):
    val base    = tw("px-4 py-2")
    val colors  = tw("bg-blue-500")
    val classes = TwClasses()
      .add(base)
      .addIf(true, colors)
      .build()
    classes.classes shouldBe "px-4 py-2 bg-blue-500"

  test("TwClasses isEmpty and nonEmpty"):
    val empty    = TwClasses()
    val nonEmpty = TwClasses().add("flex")
    empty.isEmpty shouldBe true
    empty.nonEmpty shouldBe false
    nonEmpty.isEmpty shouldBe false
    nonEmpty.nonEmpty shouldBe true

  test("TwClasses clear"):
    val builder = TwClasses().add("flex").add("items-center")
    builder.clear()
    builder.isEmpty shouldBe true

  test("TwClasses toClasses"):
    val builder = TwClasses().add("flex").add("items-center")
    builder.toClasses shouldBe "flex items-center"

  test("TwClasses factory with initial string"):
    val classes = TwClasses("flex items-center")
      .add("gap-4")
      .build()
    classes.classes shouldBe "flex items-center gap-4"

  test("TwClasses factory with initial TwStyle"):
    val base    = tw("flex items-center")
    val classes = TwClasses(base)
      .add("gap-4")
      .build()
    classes.classes shouldBe "flex items-center gap-4"

  // Example: Component style patterns

  test("component style composition"):
    object ButtonStyles:
      val base    = tw("inline-flex items-center justify-center font-medium rounded")
      val sm      = tw("px-3 py-1.5 text-sm")
      val md      = tw("px-4 py-2 text-base")
      val primary = tw("bg-blue-600 text-white").hover("bg-blue-700")

    val style = ButtonStyles.base + ButtonStyles.md + ButtonStyles.primary
    style.classes shouldContain "inline-flex"
    style.classes shouldContain "px-4"
    style.classes shouldContain "bg-blue-600"
    style.classes shouldContain "hover:bg-blue-700"

  test("status badge pattern"):
    enum Status:
      case Active, Pending, Inactive

    def statusBadge(status: Status): TwStyle =
      TwClasses()
        .add("px-2 py-1 rounded-full text-xs font-medium")
        .addMatch(status) {
          case Status.Active   => "bg-green-100 text-green-800"
          case Status.Pending  => "bg-yellow-100 text-yellow-800"
          case Status.Inactive => "bg-gray-100 text-gray-500"
        }
        .build()

    statusBadge(Status.Active).classes shouldContain "bg-green-100"
    statusBadge(Status.Pending).classes shouldContain "bg-yellow-100"
    statusBadge(Status.Inactive).classes shouldContain "bg-gray-100"

end TwStyleTest
