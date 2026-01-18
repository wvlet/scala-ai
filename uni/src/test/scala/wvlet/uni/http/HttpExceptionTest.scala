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

class HttpExceptionTest extends UniTest:

  test("HttpErrorCode should identify retryable codes") {
    HttpErrorCode.ConnectionTimeout.isRetryable shouldBe true
    HttpErrorCode.ReadTimeout.isRetryable shouldBe true
    HttpErrorCode.ServerError.isRetryable shouldBe true
    HttpErrorCode.ClientError.isRetryable shouldBe false
    HttpErrorCode.ConnectionFailed.isRetryable shouldBe false
    HttpErrorCode.Unknown.isRetryable shouldBe false
  }

  test("should create HttpException with message") {
    val e = HttpException("Something went wrong")
    e.getMessage shouldBe "Something went wrong"
    e.errorCode shouldBe HttpErrorCode.Unknown
    e.status shouldBe None
    e.statusCode shouldBe None
  }

  test("should create HttpException with error code") {
    val e = HttpException("Connection failed", HttpErrorCode.ConnectionFailed)
    e.errorCode shouldBe HttpErrorCode.ConnectionFailed
    e.isRetryable shouldBe false
  }

  test("should create HttpException with status") {
    val e = HttpException("Not found", HttpStatus.NotFound_404)
    e.getMessage shouldBe "Not found"
    e.status shouldBe Some(HttpStatus.NotFound_404)
    e.statusCode shouldBe Some(404)
    e.errorCode shouldBe HttpErrorCode.ClientError
  }

  test("should create HttpException from response") {
    val response = HttpResponse.notFound("Resource not found")
    val e        = HttpException.fromResponse(response)
    e.status shouldBe Some(HttpStatus.NotFound_404)
    e.response shouldBe Some(response)
    e.errorCode shouldBe HttpErrorCode.ClientError
    e.isClientError shouldBe true
  }

  test("should identify retryable exceptions") {
    val e1 = HttpException("Service unavailable", HttpStatus.ServiceUnavailable_503)
    e1.isRetryable shouldBe true

    val e2 = HttpException("Not found", HttpStatus.NotFound_404)
    e2.isRetryable shouldBe false

    val e3 = HttpException("Timeout", HttpErrorCode.ConnectionTimeout)
    e3.isRetryable shouldBe true
  }

  test("should identify client/server errors") {
    val clientError = HttpException("Bad request", HttpStatus.BadRequest_400)
    clientError.isClientError shouldBe true
    clientError.isServerError shouldBe false

    val serverError = HttpException("Server error", HttpStatus.InternalServerError_500)
    serverError.isServerError shouldBe true
    serverError.isClientError shouldBe false
  }

  test("should create connection failed exception") {
    val e = HttpException.connectionFailed("Connection refused")
    e.errorCode shouldBe HttpErrorCode.ConnectionFailed
    e.isRetryable shouldBe false
  }

  test("should create connection timeout exception") {
    val e = HttpException.connectionTimeout("Connect timeout")
    e.errorCode shouldBe HttpErrorCode.ConnectionTimeout
    e.isRetryable shouldBe true
  }

  test("should create read timeout exception") {
    val e = HttpException.readTimeout("Read timeout")
    e.errorCode shouldBe HttpErrorCode.ReadTimeout
    e.isRetryable shouldBe true
  }

  test("should create SSL error exception") {
    val e = HttpException.sslError("SSL handshake failed")
    e.errorCode shouldBe HttpErrorCode.SslError
    e.isRetryable shouldBe false
  }

  test("should create client error exception") {
    val e = HttpException.clientError("Bad request", HttpStatus.BadRequest_400)
    e.errorCode shouldBe HttpErrorCode.ClientError
    e.status shouldBe Some(HttpStatus.BadRequest_400)
    e.isClientError shouldBe true
  }

  test("should create server error exception") {
    val e = HttpException.serverError("Server error", HttpStatus.InternalServerError_500)
    e.errorCode shouldBe HttpErrorCode.ServerError
    e.status shouldBe Some(HttpStatus.InternalServerError_500)
    e.isServerError shouldBe true
  }

  test("should get retry-after from response") {
    val response = HttpResponse(HttpStatus.TooManyRequests_429).addHeader("Retry-After", "60")
    val e        = HttpException.fromResponse(response)
    e.retryAfter shouldBe Some(60L)
  }

  test("should return None for retry-after when header missing") {
    val response = HttpResponse(HttpStatus.TooManyRequests_429)
    val e        = HttpException.fromResponse(response)
    e.retryAfter shouldBe None
  }

  test("should preserve cause in exceptions") {
    val cause = RuntimeException("Root cause")
    val e     = HttpException("Wrapper", cause)
    e.getCause shouldBe cause
  }

  test("should preserve response in exceptions") {
    val response = HttpResponse.badRequest("Invalid data")
    val e = HttpException.clientError("Bad request", HttpStatus.BadRequest_400, Some(response))
    e.response shouldBe Some(response)
  }

end HttpExceptionTest
