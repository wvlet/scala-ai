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
import wvlet.uni.dom.CssStyles.*

class CssStylesTest extends UniTest:

  test("create style property"):
    val attr = display := "flex"
    attr.name shouldBe "style"
    attr.v shouldBe "display: flex;"
    attr.append shouldBe true

  test("multiple style properties"):
    val elem      = div(display := "flex", gap := "8px", color := "blue")
    val (node, _) = DomRenderer.createNode(elem)
    val html      = DomRenderer.renderToHtml(node)
    html shouldContain "display: flex"
    html shouldContain "gap: 8px"
    html shouldContain "color: blue"

  test("raw css string"):
    val elem      = div(css := "margin: 10px; padding: 5px;")
    val (node, _) = DomRenderer.createNode(elem)
    val html      = DomRenderer.renderToHtml(node)
    html shouldContain "margin: 10px"
    html shouldContain "padding: 5px"

  test("combine type-safe and raw styles"):
    val elem      = div(display := "flex", css := "margin: 10px;")
    val (node, _) = DomRenderer.createNode(elem)
    val html      = DomRenderer.renderToHtml(node)
    html shouldContain "display: flex"
    html shouldContain "margin: 10px"

  test("flexbox properties"):
    val elem = div(
      display        := "flex",
      flexDirection  := "column",
      justifyContent := "center",
      alignItems     := "stretch"
    )
    val (node, _) = DomRenderer.createNode(elem)
    val html      = DomRenderer.renderToHtml(node)
    html shouldContain "flex-direction: column"
    html shouldContain "justify-content: center"
    html shouldContain "align-items: stretch"

  test("box model properties"):
    val elem = div(
      CssStyles.width  := "100px",
      CssStyles.height := "50px",
      margin           := "10px",
      padding          := "5px"
    )
    val (node, _) = DomRenderer.createNode(elem)
    val html      = DomRenderer.renderToHtml(node)
    html shouldContain "width: 100px"
    html shouldContain "height: 50px"

  test("typography properties"):
    val elem = div(
      CssStyles.fontSize   := "16px",
      CssStyles.fontWeight := "bold",
      textAlign            := "center"
    )
    val (node, _) = DomRenderer.createNode(elem)
    val html      = DomRenderer.renderToHtml(node)
    html shouldContain "font-size: 16px"
    html shouldContain "font-weight: bold"
    html shouldContain "text-align: center"

end CssStylesTest
