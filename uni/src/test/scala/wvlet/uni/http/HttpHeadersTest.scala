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

class HttpHeadersTest extends UniTest:

  test("should create empty headers") {
    val headers = HttpHeaders.empty
    headers.isEmpty shouldBe true
    headers.size shouldBe 0
  }

  test("should create headers from tuples") {
    val headers = HttpHeaders("Content-Type" -> "application/json", "Accept" -> "text/html")
    headers.size shouldBe 2
    headers.get("Content-Type") shouldBe Some("application/json")
    headers.get("Accept") shouldBe Some("text/html")
  }

  test("should get header values case-insensitively") {
    val headers = HttpHeaders("Content-Type" -> "application/json")
    headers.get("Content-Type") shouldBe Some("application/json")
    headers.get("content-type") shouldBe Some("application/json")
    headers.get("CONTENT-TYPE") shouldBe Some("application/json")
  }

  test("should add headers") {
    val headers = HttpHeaders
      .empty
      .add("Content-Type", "application/json")
      .add("Accept", "text/html")
    headers.size shouldBe 2
  }

  test("should allow multiple values for same header name") {
    val headers = HttpHeaders
      .empty
      .add("Set-Cookie", "cookie1=value1")
      .add("Set-Cookie", "cookie2=value2")
    headers.getAll("Set-Cookie") shouldBe Seq("cookie1=value1", "cookie2=value2")
  }

  test("should set header replacing existing") {
    val headers = HttpHeaders("Content-Type" -> "text/plain").set(
      "Content-Type",
      "application/json"
    )
    headers.size shouldBe 1
    headers.get("Content-Type") shouldBe Some("application/json")
  }

  test("should remove headers") {
    val headers = HttpHeaders("Content-Type" -> "application/json", "Accept" -> "text/html").remove(
      "Content-Type"
    )
    headers.size shouldBe 1
    headers.contains("Content-Type") shouldBe false
    headers.contains("Accept") shouldBe true
  }

  test("should add all headers") {
    val headers1 = HttpHeaders("Content-Type" -> "application/json")
    val headers2 = HttpHeaders("Accept" -> "text/html")
    val combined = headers1.addAll(headers2)
    combined.size shouldBe 2
    combined.get("Content-Type") shouldBe Some("application/json")
    combined.get("Accept") shouldBe Some("text/html")
  }

  test("should check if header exists") {
    val headers = HttpHeaders("Content-Type" -> "application/json")
    headers.contains("Content-Type") shouldBe true
    headers.contains("Accept") shouldBe false
  }

  test("should parse content type header") {
    val headers = HttpHeaders("Content-Type" -> "application/json; charset=UTF-8")
    val ct      = headers.contentType
    ct.isDefined shouldBe true
    ct.get.fullType shouldBe "application/json"
    ct.get.charset shouldBe Some("UTF-8")
  }

  test("should parse content length header") {
    val headers = HttpHeaders("Content-Length" -> "1234")
    headers.contentLength shouldBe Some(1234L)
  }

  test("should return None for missing content length") {
    val headers = HttpHeaders.empty
    headers.contentLength shouldBe None
  }

  test("should get common headers") {
    val headers = HttpHeaders(
      "Host"          -> "example.com",
      "User-Agent"    -> "TestClient/1.0",
      "Authorization" -> "Bearer token123",
      "Accept"        -> "application/json"
    )
    headers.host shouldBe Some("example.com")
    headers.userAgent shouldBe Some("TestClient/1.0")
    headers.authorization shouldBe Some("Bearer token123")
    headers.accept shouldBe Some("application/json")
  }

  test("should convert to map") {
    val headers = HttpHeaders("Content-Type" -> "application/json", "Accept" -> "text/html")
    val map     = headers.toMap
    map.size shouldBe 2
    map.get("Content-Type") shouldBe Some("application/json")
    map.get("Accept") shouldBe Some("text/html")
  }

  test("should convert to multi-map") {
    val headers = HttpHeaders
      .empty
      .add("Set-Cookie", "cookie1=value1")
      .add("Set-Cookie", "cookie2=value2")
    val multiMap = headers.toMultiMap
    multiMap.get("set-cookie").isDefined shouldBe true
    multiMap("set-cookie").size shouldBe 2
  }

  test("should convert to seq") {
    val headers = HttpHeaders("Content-Type" -> "application/json")
    val seq     = headers.toSeq
    seq shouldBe Seq("Content-Type" -> "application/json")
  }

  test("should create headers from map") {
    val headers = HttpHeaders.fromMap(Map("Content-Type" -> "application/json"))
    headers.get("Content-Type") shouldBe Some("application/json")
  }

  test("should create headers from seq") {
    val headers = HttpHeaders.fromSeq(Seq("Content-Type" -> "application/json"))
    headers.get("Content-Type") shouldBe Some("application/json")
  }

  test("should format headers as string") {
    val headers = HttpHeaders("Content-Type" -> "application/json", "Accept" -> "text/html")
    val str     = headers.toString
    str shouldContain "Content-Type: application/json"
    str shouldContain "Accept: text/html"
  }

  test("HttpHeader should have standard header names") {
    HttpHeader.ContentType shouldBe "Content-Type"
    HttpHeader.Accept shouldBe "Accept"
    HttpHeader.Authorization shouldBe "Authorization"
    HttpHeader.Host shouldBe "Host"
    HttpHeader.UserAgent shouldBe "User-Agent"
    HttpHeader.ContentLength shouldBe "Content-Length"
    HttpHeader.Location shouldBe "Location"
    HttpHeader.Cookie shouldBe "Cookie"
    HttpHeader.SetCookie shouldBe "Set-Cookie"
  }

end HttpHeadersTest
