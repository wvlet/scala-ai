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

class HttpClientConfigTest extends UniTest:

  test("should have default configuration") {
    val config = HttpClientConfig.default
    config.baseUri shouldBe None
    config.connectTimeoutMillis shouldBe 30000
    config.readTimeoutMillis shouldBe 60000
    config.followRedirects shouldBe true
    config.maxRedirects shouldBe 10
  }

  test("should set base URI") {
    val config = HttpClientConfig.default.withBaseUri("https://api.example.com")
    config.baseUri shouldBe Some("https://api.example.com")
  }

  test("should clear base URI") {
    val config = HttpClientConfig.default.withBaseUri("https://api.example.com").noBaseUri
    config.baseUri shouldBe None
  }

  test("should set timeouts") {
    val config = HttpClientConfig
      .default
      .withConnectTimeoutMillis(5000)
      .withReadTimeoutMillis(10000)
    config.connectTimeoutMillis shouldBe 5000
    config.readTimeoutMillis shouldBe 10000
  }

  test("should configure redirects") {
    val config1 = HttpClientConfig.default.noFollowRedirects
    config1.followRedirects shouldBe false

    val config2 = config1.withFollowRedirects
    config2.followRedirects shouldBe true

    val config3 = HttpClientConfig.default.withMaxRedirects(5)
    config3.maxRedirects shouldBe 5
  }

  test("should configure retry") {
    val config1 = HttpClientConfig.default.noRetry
    config1.retryConfig.maxRetries shouldBe 0

    val retryConfig = HttpRetryConfig(maxRetries = 5, initialDelayMillis = 500)
    val config2     = HttpClientConfig.default.withRetryConfig(retryConfig)
    config2.retryConfig.maxRetries shouldBe 5
    config2.retryConfig.initialDelayMillis shouldBe 500
  }

  test("should set request filter") {
    val filter = (req: HttpRequest) => req.addHeader("X-Custom", "value")
    val config = HttpClientConfig.default.withRequestFilter(filter)

    val request  = HttpRequest.get("/test")
    val filtered = config.requestFilter(request)
    filtered.header("X-Custom") shouldBe Some("value")
  }

  test("should add request filter") {
    val filter1 = (req: HttpRequest) => req.addHeader("X-First", "1")
    val filter2 = (req: HttpRequest) => req.addHeader("X-Second", "2")
    val config  = HttpClientConfig.default.withRequestFilter(filter1).addRequestFilter(filter2)

    val request  = HttpRequest.get("/test")
    val filtered = config.requestFilter(request)
    filtered.header("X-First") shouldBe Some("1")
    filtered.header("X-Second") shouldBe Some("2")
  }

  test("should chain request filters") {
    val config = HttpClientConfig
      .default
      .addRequestFilter(_.addHeader("Accept", "application/json"))
      .addRequestFilter(_.addHeader("User-Agent", "MyApp/1.0"))

    val request  = HttpRequest.get("/test")
    val filtered = config.requestFilter(request)
    filtered.header("Accept") shouldBe Some("application/json")
    filtered.header("User-Agent") shouldBe Some("MyApp/1.0")
  }

  test("should resolve URI with base URI") {
    val config = HttpClientConfig.default.withBaseUri("https://api.example.com")
    config.resolveUri("/users") shouldBe "https://api.example.com/users"
    config.resolveUri("users") shouldBe "https://api.example.com/users"
  }

  test("should resolve URI with trailing slash in base") {
    val config = HttpClientConfig.default.withBaseUri("https://api.example.com/")
    config.resolveUri("/users") shouldBe "https://api.example.com/users"
    config.resolveUri("users") shouldBe "https://api.example.com/users"
  }

  test("should not resolve absolute URIs") {
    val config = HttpClientConfig.default.withBaseUri("https://api.example.com")
    config.resolveUri("https://other.com/path") shouldBe "https://other.com/path"
    config.resolveUri("http://other.com/path") shouldBe "http://other.com/path"
  }

  test("should return URI as-is when no base URI") {
    val config = HttpClientConfig.default
    config.resolveUri("/users") shouldBe "/users"
  }

  // HttpRetryConfig tests
  test("should have default retry configuration") {
    val config = HttpRetryConfig.default
    config.maxRetries shouldBe 3
    config.initialDelayMillis shouldBe 1000
    config.maxDelayMillis shouldBe 30000
    config.backoffMultiplier shouldBe 2.0
  }

  test("should have no-retry configuration") {
    val config = HttpRetryConfig.noRetry
    config.maxRetries shouldBe 0
  }

  test("should configure retry settings") {
    val config = HttpRetryConfig
      .default
      .withMaxRetries(5)
      .withInitialDelayMillis(500)
      .withMaxDelayMillis(60000)
      .withBackoffMultiplier(1.5)
    config.maxRetries shouldBe 5
    config.initialDelayMillis shouldBe 500
    config.maxDelayMillis shouldBe 60000
    config.backoffMultiplier shouldBe 1.5
  }

  test("should identify retryable statuses") {
    val config = HttpRetryConfig.default
    config.isRetryable(HttpStatus.ServiceUnavailable_503) shouldBe true
    config.isRetryable(HttpStatus.TooManyRequests_429) shouldBe true
    config.isRetryable(HttpStatus.NotFound_404) shouldBe false
  }

  test("should configure retryable statuses") {
    val config = HttpRetryConfig.default.withRetryableStatuses(Set(500, 502))
    config.isRetryable(HttpStatus.InternalServerError_500) shouldBe true
    config.isRetryable(HttpStatus.BadGateway_502) shouldBe true
    config.isRetryable(HttpStatus.ServiceUnavailable_503) shouldBe false
  }

  test("should calculate delay with exponential backoff") {
    val config = HttpRetryConfig(
      initialDelayMillis = 1000,
      backoffMultiplier = 2.0,
      maxDelayMillis = 30000
    )
    config.delayForAttempt(0) shouldBe 1000
    config.delayForAttempt(1) shouldBe 2000
    config.delayForAttempt(2) shouldBe 4000
    config.delayForAttempt(3) shouldBe 8000
  }

  test("should cap delay at max delay") {
    val config = HttpRetryConfig(
      initialDelayMillis = 10000,
      backoffMultiplier = 2.0,
      maxDelayMillis = 15000
    )
    config.delayForAttempt(0) shouldBe 10000
    config.delayForAttempt(1) shouldBe 15000 // capped
    config.delayForAttempt(2) shouldBe 15000 // capped
  }

end HttpClientConfigTest
