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

class HttpMultiMapTest extends UniTest:

  test("should create empty map") {
    val map = HttpMultiMap.empty
    map.isEmpty shouldBe true
    map.size shouldBe 0
  }

  test("should create from varargs") {
    val map = HttpMultiMap("Content-Type" -> "application/json", "Accept" -> "text/html")
    map.size shouldBe 2
    map.get("Content-Type") shouldBe Some("application/json")
  }

  test("should be case-insensitive for get") {
    val map = HttpMultiMap("Content-Type" -> "application/json")
    map.get("content-type") shouldBe Some("application/json")
    map.get("CONTENT-TYPE") shouldBe Some("application/json")
    map.get("Content-Type") shouldBe Some("application/json")
  }

  test("should be case-insensitive for contains") {
    val map = HttpMultiMap("Content-Type" -> "application/json")
    map.contains("content-type") shouldBe true
    map.contains("CONTENT-TYPE") shouldBe true
    map.contains("Content-Type") shouldBe true
  }

  test("should add multiple values for same key") {
    val map = HttpMultiMap
      .empty
      .add("Set-Cookie", "cookie1=value1")
      .add("Set-Cookie", "cookie2=value2")
    map.get("Set-Cookie") shouldBe Some("cookie1=value1")
    map.getAll("Set-Cookie") shouldBe Seq("cookie1=value1", "cookie2=value2")
  }

  test("should set replaces existing values") {
    val map = HttpMultiMap
      .empty
      .add("Accept", "text/html")
      .add("Accept", "application/json")
      .set("Accept", "text/plain")
    map.getAll("Accept") shouldBe Seq("text/plain")
  }

  test("should remove key case-insensitively") {
    val map = HttpMultiMap("Content-Type" -> "application/json", "Accept" -> "text/html").remove(
      "content-type"
    )
    map.contains("Content-Type") shouldBe false
    map.contains("Accept") shouldBe true
  }

  test("should preserve original key casing in entries") {
    val map = HttpMultiMap
      .empty
      .add("Content-Type", "application/json")
      .add("X-Custom-Header", "value")
    val entries = map.entries
    entries.exists(_._1 == "Content-Type") shouldBe true
    entries.exists(_._1 == "X-Custom-Header") shouldBe true
  }

  test("should support + operator") {
    val map = HttpMultiMap.empty + ("Content-Type" -> "application/json")
    map.get("Content-Type") shouldBe Some("application/json")
  }

  test("should support ++ operator") {
    val map1     = HttpMultiMap("Content-Type" -> "application/json")
    val map2     = HttpMultiMap("Accept" -> "text/html")
    val combined = map1 ++ map2
    combined.get("Content-Type") shouldBe Some("application/json")
    combined.get("Accept") shouldBe Some("text/html")
  }

  test("should return default with getOrElse") {
    val map = HttpMultiMap.empty
    map.getOrElse("Missing", "default") shouldBe "default"
  }

  test("should create from seq") {
    val map = HttpMultiMap.fromSeq(Seq("A" -> "1", "B" -> "2", "A" -> "3"))
    map.getAll("A") shouldBe Seq("1", "3")
    map.get("B") shouldBe Some("2")
  }

  test("should create from map") {
    val map = HttpMultiMap.fromMap(Map("A" -> "1", "B" -> "2"))
    map.get("A") shouldBe Some("1")
    map.get("B") shouldBe Some("2")
  }

  test("should create from multi-map") {
    val map = HttpMultiMap.fromMultiMap(Map("A" -> Seq("1", "2"), "B" -> Seq("3")))
    map.getAll("A") shouldBe Seq("1", "2")
    map.getAll("B") shouldBe Seq("3")
  }

  test("should use builder pattern") {
    val map = HttpMultiMap
      .newBuilder
      .add("Content-Type", "application/json")
      .add("Accept", "text/html")
      .result()
    map.size shouldBe 2
    map.get("Content-Type") shouldBe Some("application/json")
  }

  test("should return empty seq for missing key in getAll") {
    val map = HttpMultiMap.empty
    map.getAll("Missing") shouldBe Seq.empty
  }

  test("should convert to raw map") {
    val map = HttpMultiMap("Content-Type" -> "application/json")
    val raw = map.toMap
    raw.get("content-type") shouldBe Some(Seq("application/json"))
  }

  test("should provide keys in original casing") {
    val map  = HttpMultiMap("Content-Type" -> "application/json", "X-Custom" -> "value")
    val keys = map.keys.toSet
    keys.contains("Content-Type") shouldBe true
    keys.contains("X-Custom") shouldBe true
  }

end HttpMultiMapTest
