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
package wvlet.uni.http

import wvlet.uni.test.UniTest
import wvlet.uni.json.JSON

class HttpContentTest extends UniTest:

  test("should create empty content") {
    val content = HttpContent.empty
    content.isEmpty shouldBe true
    content.nonEmpty shouldBe false
    content.length shouldBe 0
    content.contentType shouldBe None
    content.asString shouldBe None
    content.asBytes shouldBe None
  }

  test("should create text content") {
    val content = HttpContent.text("Hello World")
    content.isEmpty shouldBe false
    content.nonEmpty shouldBe true
    content.asString shouldBe Some("Hello World")
    content.asBytes.isDefined shouldBe true
    content.contentType shouldBe Some(ContentType.TextPlain)
    (content.length > 0) shouldBe true
  }

  test("should create text content with custom content type") {
    val content = HttpContent.text("<html></html>", ContentType.TextHtml)
    content.contentType shouldBe Some(ContentType.TextHtml)
  }

  test("should create empty content for empty string") {
    val content = HttpContent.text("")
    content.isEmpty shouldBe true
    content shouldBe HttpContent.Empty
  }

  test("should create bytes content") {
    val bytes   = "Hello".getBytes("UTF-8")
    val content = HttpContent.bytes(bytes)
    content.isEmpty shouldBe false
    content.asBytes shouldBe Some(bytes)
    content.asString shouldBe Some("Hello")
    content.contentType shouldBe Some(ContentType.ApplicationOctetStream)
    content.length shouldBe bytes.length
  }

  test("should create bytes content with custom content type") {
    val bytes   = Array[Byte](1, 2, 3)
    val content = HttpContent.bytes(bytes, ContentType.ImagePng)
    content.contentType shouldBe Some(ContentType.ImagePng)
  }

  test("should create empty content for empty bytes") {
    val content = HttpContent.bytes(Array.empty[Byte])
    content.isEmpty shouldBe true
    content shouldBe HttpContent.Empty
  }

  test("should create JSON content from JSONValue") {
    val json    = JSON.parse("""{"name": "John"}""")
    val content = HttpContent.json(json)
    content.isEmpty shouldBe false
    content.contentType shouldBe Some(ContentType.ApplicationJson)
    content.asString.get shouldContain "John"
    (content.length > 0) shouldBe true
  }

  test("should create JSON content from string") {
    val content = HttpContent.json("""{"status": "ok"}""")
    content.contentType.isDefined shouldBe true
    content.contentType.get.isJson shouldBe true
  }

  test("should create HTML content") {
    val content = HttpContent.html("<html><body>Hello</body></html>")
    content.contentType shouldBe Some(ContentType.TextHtml)
    content.asString.get shouldContain "<html>"
  }

  test("should create XML content") {
    val content = HttpContent.xml("<root><item>1</item></root>")
    content.contentType shouldBe Some(ContentType.ApplicationXml)
    content.asString.get shouldContain "<root>"
  }

  test("TextContent should report correct length") {
    val content = HttpContent.text("Hello")
    content.length shouldBe 5
  }

  test("ByteContent should report correct length") {
    val bytes   = Array[Byte](1, 2, 3, 4, 5)
    val content = HttpContent.bytes(bytes)
    content.length shouldBe 5
  }

  test("JsonContent should never be empty") {
    val json    = JSON.parse("{}")
    val content = HttpContent.json(json)
    content.isEmpty shouldBe false
  }

  test("should convert bytes to string") {
    val bytes   = "Hello".getBytes("UTF-8")
    val content = HttpContent.bytes(bytes)
    content.asString shouldBe Some("Hello")
  }

  test("should convert text to bytes") {
    val content = HttpContent.text("Hello")
    val bytes   = content.asBytes.get
    String(bytes, "UTF-8") shouldBe "Hello"
  }

  test("toContentString should return empty string for Empty") {
    HttpContent.empty.toContentString shouldBe ""
  }

  test("toContentString should return content for TextContent") {
    val content = HttpContent.text("Hello World")
    content.toContentString shouldBe "Hello World"
  }

  test("toContentString should return decoded string for ByteContent") {
    val bytes   = "Hello Bytes".getBytes("UTF-8")
    val content = HttpContent.bytes(bytes)
    content.toContentString shouldBe "Hello Bytes"
  }

  test("toContentString should return JSON string for JsonContent") {
    val json    = JSON.parse("""{"key": "value"}""")
    val content = HttpContent.json(json)
    content.toContentString shouldContain "key"
    content.toContentString shouldContain "value"
  }

  test("toContentBytes should return empty array for Empty") {
    val bytes = HttpContent.empty.toContentBytes
    bytes.length shouldBe 0
  }

  test("toContentBytes should return bytes for TextContent") {
    val content = HttpContent.text("Hello")
    val bytes   = content.toContentBytes
    String(bytes, "UTF-8") shouldBe "Hello"
  }

  test("toContentBytes should return bytes for ByteContent") {
    val originalBytes = Array[Byte](1, 2, 3, 4, 5)
    val content       = HttpContent.bytes(originalBytes)
    content.toContentBytes shouldBe originalBytes
  }

  test("toContentBytes should return bytes for JsonContent") {
    val json    = JSON.parse("""{"a": 1}""")
    val content = HttpContent.json(json)
    val bytes   = content.toContentBytes
    (bytes.length > 0) shouldBe true
    String(bytes, "UTF-8") shouldContain "a"
  }

  test("contentHash should return 0 for Empty") {
    HttpContent.empty.contentHash shouldBe 0
  }

  test("contentHash should be consistent for same content") {
    val content1 = HttpContent.text("Hello")
    val content2 = HttpContent.text("Hello")
    content1.contentHash shouldBe content2.contentHash
  }

  test("contentHash should differ for different content") {
    val content1 = HttpContent.text("Hello")
    val content2 = HttpContent.text("World")
    (content1.contentHash != content2.contentHash) shouldBe true
  }

  test("contentHash should work for ByteContent") {
    val content = HttpContent.bytes(Array[Byte](1, 2, 3))
    (content.contentHash != 0) shouldBe true
  }

  test("contentHash should work for JsonContent") {
    val json    = JSON.parse("""{"test": true}""")
    val content = HttpContent.json(json)
    (content.contentHash != 0) shouldBe true
  }

end HttpContentTest
