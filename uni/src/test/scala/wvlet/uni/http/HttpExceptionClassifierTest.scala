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

import wvlet.uni.control.CircuitBreaker
import wvlet.uni.control.CircuitBreakerOpenException
import wvlet.uni.control.ResultClass
import wvlet.uni.test.UniTest

import java.io.EOFException
import java.util.concurrent.TimeoutException

class HttpExceptionClassifierTest extends UniTest:

  // Response classification tests
  test("classify 2xx responses as Succeeded") {
    val response = Response.ok("Success")
    HttpExceptionClassifier.classifyHttpResponse(response) shouldBe ResultClass.Succeeded
  }

  test("classify 304 Not Modified as Succeeded") {
    val response = Response.notModified
    HttpExceptionClassifier.classifyHttpResponse(response) shouldBe ResultClass.Succeeded
  }

  test("classify 5xx as retryable failure") {
    val response = Response.internalServerError("Error")
    val result   = HttpExceptionClassifier.classifyHttpResponse(response)
    result shouldMatch { case ResultClass.Failed(isRetryable, _, _) =>
      isRetryable shouldBe true
    }
  }

  test("classify 503 Service Unavailable with extra wait factor") {
    val response = Response.serviceUnavailable
    val result   = HttpExceptionClassifier.classifyHttpResponse(response)
    result shouldMatch { case f: ResultClass.Failed =>
      f.isRetryable shouldBe true
      f.extraWait.factor shouldBe 0.5
    }
  }

  test("classify 408 Request Timeout as retryable") {
    val response = Response(HttpStatus.RequestTimeout_408)
    val result   = HttpExceptionClassifier.classifyHttpResponse(response)
    result shouldMatch { case ResultClass.Failed(isRetryable, _, _) =>
      isRetryable shouldBe true
    }
  }

  test("classify 410 Gone as retryable") {
    val response = Response.gone
    val result   = HttpExceptionClassifier.classifyHttpResponse(response)
    result shouldMatch { case ResultClass.Failed(isRetryable, _, _) =>
      isRetryable shouldBe true
    }
  }

  test("classify 429 Too Many Requests as retryable") {
    val response = Response.tooManyRequests
    val result   = HttpExceptionClassifier.classifyHttpResponse(response)
    result shouldMatch { case ResultClass.Failed(isRetryable, _, _) =>
      isRetryable shouldBe true
    }
  }

  test("classify 499 Client Closed Request as retryable") {
    val response = Response(HttpStatus.ClientClosedRequest_499)
    val result   = HttpExceptionClassifier.classifyHttpResponse(response)
    result shouldMatch { case ResultClass.Failed(isRetryable, _, _) =>
      isRetryable shouldBe true
    }
  }

  test("classify 400 with retryable message as retryable") {
    val response = Response.badRequest("Idle connections will be closed")
    val result   = HttpExceptionClassifier.classifyHttpResponse(response)
    result shouldMatch { case ResultClass.Failed(isRetryable, _, _) =>
      isRetryable shouldBe true
    }
  }

  test("classify regular 400 as non-retryable") {
    val response = Response.badRequest("Invalid request")
    val result   = HttpExceptionClassifier.classifyHttpResponse(response)
    result shouldMatch { case ResultClass.Failed(isRetryable, _, _) =>
      isRetryable shouldBe false
    }
  }

  test("classify 404 as non-retryable") {
    val response = Response.notFound("Not found")
    val result   = HttpExceptionClassifier.classifyHttpResponse(response)
    result shouldMatch { case ResultClass.Failed(isRetryable, _, _) =>
      isRetryable shouldBe false
    }
  }

  test("classify 401 as non-retryable") {
    val response = Response.unauthorized
    val result   = HttpExceptionClassifier.classifyHttpResponse(response)
    result shouldMatch { case ResultClass.Failed(isRetryable, _, _) =>
      isRetryable shouldBe false
    }
  }

  test("classify 403 as non-retryable") {
    val response = Response.forbidden
    val result   = HttpExceptionClassifier.classifyHttpResponse(response)
    result shouldMatch { case ResultClass.Failed(isRetryable, _, _) =>
      isRetryable shouldBe false
    }
  }

  // Exception classification tests
  test("classify CircuitBreakerOpenException as non-retryable") {
    val ctx    = CircuitBreaker.default
    val ex     = CircuitBreakerOpenException(ctx)
    val result = HttpExceptionClassifier.classifyExecutionFailure(ex)
    result.isRetryable shouldBe false
  }

  test("classify EOFException as retryable") {
    val ex     = EOFException("Connection closed")
    val result = HttpExceptionClassifier.classifyExecutionFailure(ex)
    result.isRetryable shouldBe true
  }

  test("classify TimeoutException as retryable") {
    val ex     = TimeoutException("Timeout")
    val result = HttpExceptionClassifier.classifyExecutionFailure(ex)
    result.isRetryable shouldBe true
  }

  test("classify retryable HttpException as retryable") {
    val ex     = HttpException.connectionTimeout("Timeout")
    val result = HttpExceptionClassifier.classifyExecutionFailure(ex)
    result.isRetryable shouldBe true
  }

  test("classify non-retryable HttpException as non-retryable") {
    val ex     = HttpException.clientError("Bad request", HttpStatus.BadRequest_400)
    val result = HttpExceptionClassifier.classifyExecutionFailure(ex)
    result.isRetryable shouldBe false
  }

  test("classify unknown exception as non-retryable") {
    val ex     = RuntimeException("Unknown error")
    val result = HttpExceptionClassifier.classifyExecutionFailure(ex)
    result.isRetryable shouldBe false
  }

  test("classify exception with retryable cause via root cause unwrapping") {
    val cause   = EOFException("Connection closed")
    val wrapper = RuntimeException("Wrapped", cause)
    val result  = HttpExceptionClassifier.classifyExecutionFailure(wrapper)
    result.isRetryable shouldBe true
  }

end HttpExceptionClassifierTest
