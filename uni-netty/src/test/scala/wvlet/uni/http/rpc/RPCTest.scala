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
package wvlet.uni.http.rpc

import wvlet.uni.http.{HttpHeader, HttpStatus}
import wvlet.uni.test.UniTest

// Test RPC service - no annotation needed
trait GreeterService:
  def hello(name: String): String
  def greet(greeting: String, name: String): String
  def add(a: Int, b: Int): Int
  def echo(message: String): String

class RPCTest extends UniTest:

  test("RPCStatus should have correct codes") {
    RPCStatus.SUCCESS_S0.code shouldBe 0
    RPCStatus.INVALID_REQUEST_U1.code shouldBe 1001
    RPCStatus.NOT_FOUND_U5.code shouldBe 1005
    RPCStatus.INTERNAL_ERROR_I0.code shouldBe 2000
    RPCStatus.RESOURCE_EXHAUSTED_R0.code shouldBe 3000
  }

  test("RPCStatus should have correct status types") {
    RPCStatus.SUCCESS_S0.statusType shouldBe RPCStatusType.SUCCESS
    RPCStatus.INVALID_REQUEST_U1.statusType shouldBe RPCStatusType.USER_ERROR
    RPCStatus.INTERNAL_ERROR_I0.statusType shouldBe RPCStatusType.INTERNAL_ERROR
    RPCStatus.RESOURCE_EXHAUSTED_R0.statusType shouldBe RPCStatusType.RESOURCE_EXHAUSTED
  }

  test("RPCStatus should map to correct HTTP status") {
    RPCStatus.SUCCESS_S0.httpStatus shouldBe HttpStatus.Ok_200
    RPCStatus.INVALID_REQUEST_U1.httpStatus shouldBe HttpStatus.BadRequest_400
    RPCStatus.NOT_FOUND_U5.httpStatus shouldBe HttpStatus.NotFound_404
    RPCStatus.UNAUTHENTICATED_U13.httpStatus shouldBe HttpStatus.Unauthorized_401
    RPCStatus.INTERNAL_ERROR_I0.httpStatus shouldBe HttpStatus.InternalServerError_500
    RPCStatus.RESOURCE_EXHAUSTED_R0.httpStatus shouldBe HttpStatus.TooManyRequests_429
  }

  test("RPCStatus.ofCode should return correct status") {
    RPCStatus.ofCode(0) shouldBe RPCStatus.SUCCESS_S0
    RPCStatus.ofCode(1001) shouldBe RPCStatus.INVALID_REQUEST_U1
    RPCStatus.ofCode(2000) shouldBe RPCStatus.INTERNAL_ERROR_I0
  }

  test("RPCStatus.ofCodeName should return correct status") {
    RPCStatus.ofCodeName("SUCCESS_S0") shouldBe RPCStatus.SUCCESS_S0
    RPCStatus.ofCodeName("INVALID_REQUEST_U1") shouldBe RPCStatus.INVALID_REQUEST_U1
  }

  test("RPCException should create from status") {
    val ex = RPCStatus.INVALID_REQUEST_U1.newException("test error")
    ex.status shouldBe RPCStatus.INVALID_REQUEST_U1
    ex.message shouldBe "test error"
  }

  test("RPCException should serialize to JSON") {
    val ex   = RPCStatus.NOT_FOUND_U5.newException("resource not found")
    val json = ex.toJson
    json shouldContain "1005"
    json shouldContain "NOT_FOUND_U5"
    json shouldContain "resource not found"
  }

  test("RPCException should deserialize from JSON") {
    val original = RPCStatus.INVALID_ARGUMENT_U2.newException("bad argument")
    val json     = original.toJson
    val restored = RPCException.fromJson(json)
    restored.status shouldBe original.status
    restored.message shouldBe original.message
  }

  test("RPCException should convert to Response") {
    val ex       = RPCStatus.NOT_FOUND_U5.newException("not found")
    val response = ex.toResponse
    response.status shouldBe HttpStatus.NotFound_404
    response.header(HttpHeader.XRPCStatus) shouldBe Some(RPCStatus.NOT_FOUND_U5.code.toString)
  }

  test("RPCRouter should create routes for all public methods") {
    val router = RPCRouter.of[GreeterService]
    router.routes.size shouldBe 4

    val routePaths = router.routes.map(_.pathPattern).toSet
    routePaths shouldContain "/rpc/GreeterService/hello"
    routePaths shouldContain "/rpc/GreeterService/greet"
    routePaths shouldContain "/rpc/GreeterService/add"
    routePaths shouldContain "/rpc/GreeterService/echo"
  }

  test("RPCRouter should use custom prefix") {
    val router     = RPCRouter.of[GreeterService]("/api/v1")
    val routePaths = router.routes.map(_.pathPattern).toSet
    routePaths shouldContain "/api/v1/GreeterService/hello"
  }

end RPCTest
