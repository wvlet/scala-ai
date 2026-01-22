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
package wvlet.uni.http.router

import wvlet.uni.http.Response
import wvlet.uni.test.UniTest

/**
  * JVM-specific tests for ResponseConverter that use Rx.toSeq (not available in Scala.js/Native)
  */
class ResponseConverterJvmTest extends UniTest:

  test("ResponseConverter should convert return values") {
    test("convert String to Response") {
      val rx     = ResponseConverter.toResponse("hello")
      val result = rx.toSeq.head
      result.status.code shouldBe 200
      result.content.toContentString shouldBe "hello"
    }

    test("convert Response as-is") {
      val response = Response.notFound
      val rx       = ResponseConverter.toResponse(response)
      val result   = rx.toSeq.head
      result.status.code shouldBe 404
    }

    test("convert Unit to no content") {
      val rx     = ResponseConverter.toResponse(())
      val result = rx.toSeq.head
      result.status.code shouldBe 204
    }

    test("convert Seq to JSON array") {
      val rx     = ResponseConverter.toResponse(Seq(1, 2, 3))
      val result = rx.toSeq.head
      result.status.code shouldBe 200
      result.content.toContentString shouldBe "[1,2,3]"
    }

    test("convert Map to JSON object") {
      val rx     = ResponseConverter.toResponse(Map("key" -> "value"))
      val result = rx.toSeq.head
      result.status.code shouldBe 200
      result.content.toContentString shouldBe "{\"key\":\"value\"}"
    }
  }

end ResponseConverterJvmTest
