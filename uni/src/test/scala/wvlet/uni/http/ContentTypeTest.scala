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

class ContentTypeTest extends UniTest:

  test("should have common content types") {
    ContentType.ApplicationJson.fullType shouldBe "application/json"
    ContentType.ApplicationXml.fullType shouldBe "application/xml"
    ContentType.TextPlain.fullType shouldBe "text/plain"
    ContentType.TextHtml.fullType shouldBe "text/html"
    ContentType.ApplicationOctetStream.fullType shouldBe "application/octet-stream"
  }

  test("should get media type") {
    ContentType.ApplicationJson.mediaType shouldBe "application"
    ContentType.TextPlain.mediaType shouldBe "text"
    ContentType.ImagePng.mediaType shouldBe "image"
  }

  test("should get sub type") {
    ContentType.ApplicationJson.subType shouldBe "json"
    ContentType.TextPlain.subType shouldBe "plain"
    ContentType.ImagePng.subType shouldBe "png"
  }

  test("should add charset") {
    val ct = ContentType.TextPlain.withCharset("UTF-8")
    ct.charset shouldBe Some("UTF-8")
    ct.value shouldContain "charset=UTF-8"
  }

  test("should add UTF-8 charset") {
    val ct = ContentType.TextHtml.withUtf8Charset
    ct.charset shouldBe Some("UTF-8")
  }

  test("should add boundary") {
    val ct = ContentType.MultipartFormData.withBoundary("----FormBoundary")
    ct.value shouldContain "boundary=----FormBoundary"
  }

  test("should identify text types") {
    ContentType.TextPlain.isText shouldBe true
    ContentType.TextHtml.isText shouldBe true
    ContentType.ApplicationJson.isText shouldBe false
  }

  test("should identify application types") {
    ContentType.ApplicationJson.isApplication shouldBe true
    ContentType.ApplicationOctetStream.isApplication shouldBe true
    ContentType.TextPlain.isApplication shouldBe false
  }

  test("should identify JSON types") {
    ContentType.ApplicationJson.isJson shouldBe true
    ContentType("application/vnd.api+json").isJson shouldBe true
    ContentType.TextPlain.isJson shouldBe false
  }

  test("should identify XML types") {
    ContentType.ApplicationXml.isXml shouldBe true
    ContentType.TextXml.isXml shouldBe true
    ContentType("application/atom+xml").isXml shouldBe true
    ContentType.TextPlain.isXml shouldBe false
  }

  test("should identify multipart types") {
    ContentType.MultipartFormData.isMultipart shouldBe true
    ContentType.MultipartMixed.isMultipart shouldBe true
    ContentType.ApplicationJson.isMultipart shouldBe false
  }

  test("should parse content type strings") {
    val ct1 = ContentType.parse("application/json")
    ct1 shouldBe Some(ContentType.ApplicationJson)

    val ct2 = ContentType.parse("text/html; charset=UTF-8")
    ct2.isDefined shouldBe true
    ct2.get.fullType shouldBe "text/html"
    ct2.get.charset shouldBe Some("UTF-8")
  }

  test("should parse charset with quotes") {
    val ct = ContentType.parse("text/html; charset=\"UTF-8\"")
    ct.isDefined shouldBe true
    ct.get.charset shouldBe Some("UTF-8")
  }

  test("should return None for empty content type strings") {
    ContentType.parse("") shouldBe None
  }

  test("should support pattern matching with unapply") {
    "application/json" match
      case ContentType(ct) => ct.fullType shouldBe "application/json"
      case _               => fail("Should match")
  }

  test("should get raw value") {
    ContentType.ApplicationJson.value shouldBe "application/json"
  }

  test("should create custom content type") {
    val ct = ContentType("application/vnd.custom+json")
    ct.isJson shouldBe true
    ct.mediaType shouldBe "application"
    ct.subType shouldBe "vnd.custom+json"
  }
