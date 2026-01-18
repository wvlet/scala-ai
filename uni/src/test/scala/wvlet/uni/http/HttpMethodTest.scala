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

class HttpMethodTest extends UniTest:

  test("should have correct method names") {
    HttpMethod.GET.name shouldBe "GET"
    HttpMethod.POST.name shouldBe "POST"
    HttpMethod.PUT.name shouldBe "PUT"
    HttpMethod.DELETE.name shouldBe "DELETE"
    HttpMethod.PATCH.name shouldBe "PATCH"
    HttpMethod.HEAD.name shouldBe "HEAD"
    HttpMethod.OPTIONS.name shouldBe "OPTIONS"
    HttpMethod.TRACE.name shouldBe "TRACE"
    HttpMethod.CONNECT.name shouldBe "CONNECT"
  }

  test("should identify safe methods") {
    HttpMethod.GET.isSafe shouldBe true
    HttpMethod.HEAD.isSafe shouldBe true
    HttpMethod.OPTIONS.isSafe shouldBe true
    HttpMethod.TRACE.isSafe shouldBe true
    HttpMethod.POST.isSafe shouldBe false
    HttpMethod.PUT.isSafe shouldBe false
    HttpMethod.DELETE.isSafe shouldBe false
    HttpMethod.PATCH.isSafe shouldBe false
  }

  test("should identify idempotent methods") {
    HttpMethod.GET.isIdempotent shouldBe true
    HttpMethod.PUT.isIdempotent shouldBe true
    HttpMethod.DELETE.isIdempotent shouldBe true
    HttpMethod.HEAD.isIdempotent shouldBe true
    HttpMethod.OPTIONS.isIdempotent shouldBe true
    HttpMethod.POST.isIdempotent shouldBe false
  }

  test("should lookup method by name") {
    HttpMethod.of("GET") shouldBe Some(HttpMethod.GET)
    HttpMethod.of("post") shouldBe Some(HttpMethod.POST)
    HttpMethod.of("Put") shouldBe Some(HttpMethod.PUT)
    HttpMethod.of("INVALID") shouldBe None
  }

  test("should support pattern matching with unapply") {
    "GET" match
      case HttpMethod(method) =>
        method shouldBe HttpMethod.GET
      case _ =>
        fail("Should match")

    "INVALID" match
      case HttpMethod(_) =>
        fail("Should not match")
      case _ => // expected
  }

  test("should format as string") {
    HttpMethod.GET.toString shouldBe "GET"
    HttpMethod.POST.toString shouldBe "POST"
  }

end HttpMethodTest
