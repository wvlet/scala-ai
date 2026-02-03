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

import wvlet.uni.control.Retry
import wvlet.uni.test.UniTest

class HttpMaxRetryExceptionTest extends UniTest:

  private val retryContext = Retry
    .withBackOff(maxRetry = 3)
    .noRetryLogging
    .init()
    .nextRetry(RuntimeException("First failure"))
    .nextRetry(RuntimeException("Second failure"))

  test("create HttpMaxRetryException with response") {
    val response = Response.internalServerError("Server error")
    val cause    = RuntimeException("Test error")
    val ex       = HttpMaxRetryException(response, retryContext, cause)

    ex.response shouldBe response
    ex.status shouldBe HttpStatus.InternalServerError_500
    ex.statusCode shouldBe 500
    ex.getMessage shouldContain "max retry count"
    ex.getCause shouldBe cause
  }

  test("create HttpMaxRetryException from HttpException with response") {
    val response = Response.badGateway
    val cause    = HttpException.fromResponse(response)
    val ex       = HttpMaxRetryException.fromCause(retryContext, cause)

    ex.response shouldBe response
    ex.status shouldBe HttpStatus.BadGateway_502
    ex.statusCode shouldBe 502
  }

  test("create HttpMaxRetryException from HttpException with status only") {
    val cause = HttpException("Service unavailable", HttpStatus.ServiceUnavailable_503)
    val ex    = HttpMaxRetryException.fromCause(retryContext, cause)

    ex.status shouldBe HttpStatus.ServiceUnavailable_503
    ex.statusCode shouldBe 503
    ex.response.contentAsString shouldBe Some("Service unavailable")
  }

  test("create HttpMaxRetryException from generic exception") {
    val cause = RuntimeException("Unknown error")
    val ex    = HttpMaxRetryException.fromCause(retryContext, cause)

    ex.status shouldBe HttpStatus.InternalServerError_500
    ex.statusCode shouldBe 500
    ex.response.contentAsString shouldBe Some("Unknown error")
  }

  test("create HttpMaxRetryException from nested HttpMaxRetryException") {
    val response = Response.gatewayTimeout
    val inner    = HttpMaxRetryException(response, retryContext, RuntimeException("Inner"))
    val ex       = HttpMaxRetryException.fromCause(retryContext, inner)

    ex.response shouldBe response
    ex.status shouldBe HttpStatus.GatewayTimeout_504
  }

  test("exception message includes retry count") {
    val ex = HttpMaxRetryException(
      Response.internalServerError,
      retryContext,
      RuntimeException("Test")
    )
    ex.getMessage shouldContain "2/3"
  }

end HttpMaxRetryExceptionTest
