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
    val headers = HttpMultiMap.empty
    headers.isEmpty shouldBe true
    headers.size shouldBe 0
  }

  test("should create headers from tuples") {
    val headers = HttpMultiMap("Content-Type" -> "application/json", "Accept" -> "text/html")
    headers.size shouldBe 2
    headers.get("Content-Type") shouldBe Some("application/json")
    headers.get("Accept") shouldBe Some("text/html")
  }

  test("should get header values case-insensitively") {
    val headers = HttpMultiMap("Content-Type" -> "application/json")
    headers.get("Content-Type") shouldBe Some("application/json")
    headers.get("content-type") shouldBe Some("application/json")
    headers.get("CONTENT-TYPE") shouldBe Some("application/json")
  }

  test("should add headers") {
    val headers = HttpMultiMap
      .empty
      .add("Content-Type", "application/json")
      .add("Accept", "text/html")
    headers.size shouldBe 2
  }

  test("should allow multiple values for same header name") {
    val headers = HttpMultiMap
      .empty
      .add("Set-Cookie", "cookie1=value1")
      .add("Set-Cookie", "cookie2=value2")
    headers.getAll("Set-Cookie") shouldBe Seq("cookie1=value1", "cookie2=value2")
  }

  test("should set header replacing existing") {
    val headers = HttpMultiMap("Content-Type" -> "text/plain").set(
      "Content-Type",
      "application/json"
    )
    headers.size shouldBe 1
    headers.get("Content-Type") shouldBe Some("application/json")
  }

  test("should remove headers") {
    val headers = HttpMultiMap("Content-Type" -> "application/json", "Accept" -> "text/html")
      .remove("Content-Type")
    headers.size shouldBe 1
    headers.contains("Content-Type") shouldBe false
    headers.contains("Accept") shouldBe true
  }

  test("should add all headers with ++") {
    val headers1 = HttpMultiMap("Content-Type" -> "application/json")
    val headers2 = HttpMultiMap("Accept" -> "text/html")
    val combined = headers1 ++ headers2
    combined.size shouldBe 2
    combined.get("Content-Type") shouldBe Some("application/json")
    combined.get("Accept") shouldBe Some("text/html")
  }

  test("should check if header exists") {
    val headers = HttpMultiMap("Content-Type" -> "application/json")
    headers.contains("Content-Type") shouldBe true
    headers.contains("Accept") shouldBe false
  }

  test("should get entries preserving original case") {
    val headers = HttpMultiMap("Content-Type" -> "application/json")
    val entries = headers.entries
    entries shouldBe Seq("Content-Type" -> "application/json")
  }

  test("should create headers from map") {
    val headers = HttpMultiMap.fromMap(Map("Content-Type" -> "application/json"))
    headers.get("Content-Type") shouldBe Some("application/json")
  }

  test("should create headers from seq") {
    val headers = HttpMultiMap.fromSeq(Seq("Content-Type" -> "application/json"))
    headers.get("Content-Type") shouldBe Some("application/json")
  }

  test("should create headers from multi-map") {
    val headers = HttpMultiMap.fromMultiMap(
      Map("Set-Cookie" -> Seq("cookie1=value1", "cookie2=value2"))
    )
    headers.getAll("Set-Cookie") shouldBe Seq("cookie1=value1", "cookie2=value2")
  }

  test("should format headers as string") {
    val headers = HttpMultiMap("Content-Type" -> "application/json")
    val str     = headers.toString
    str shouldContain "Content-Type: application/json"
  }

  test("should use + operator to add entries") {
    val headers = HttpMultiMap.empty + ("Content-Type" -> "application/json")
    headers.get("Content-Type") shouldBe Some("application/json")
  }

  test("should use builder pattern") {
    val builder = HttpMultiMap.newBuilder
    builder.add("Content-Type", "application/json")
    builder += ("Accept" -> "text/html")
    val headers = builder.result()
    headers.size shouldBe 2
    headers.get("Content-Type") shouldBe Some("application/json")
    headers.get("Accept") shouldBe Some("text/html")
  }

  test("should convert to raw map") {
    val headers = HttpMultiMap("Content-Type" -> "application/json", "Accept" -> "text/html")
    val rawMap  = headers.toMap
    rawMap.size shouldBe 2
    rawMap.get("content-type") shouldBe Some(Seq("application/json"))
    rawMap.get("accept") shouldBe Some(Seq("text/html"))
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

  test("HttpHeaders alias should work") {
    // HttpHeaders is now a type alias for HttpMultiMap
    val headers: HttpHeaders = HttpMultiMap("Content-Type" -> "application/json")
    headers.get("Content-Type") shouldBe Some("application/json")
  }

end HttpHeadersTest
