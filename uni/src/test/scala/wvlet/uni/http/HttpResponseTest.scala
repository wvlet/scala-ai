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

class HttpResponseTest extends UniTest:

  test("should create OK response") {
    val response = HttpResponse.ok
    response.status shouldBe HttpStatus.Ok_200
    response.statusCode shouldBe 200
    response.isSuccessful shouldBe true
  }

  test("should create OK response with content") {
    val response = HttpResponse.ok("Hello")
    response.status shouldBe HttpStatus.Ok_200
    response.contentAsString shouldBe Some("Hello")
  }

  test("should create Created response") {
    val response = HttpResponse.created
    response.status shouldBe HttpStatus.Created_201
    response.isSuccessful shouldBe true
  }

  test("should create No Content response") {
    val response = HttpResponse.noContent
    response.status shouldBe HttpStatus.NoContent_204
  }

  test("should create redirect response") {
    val response = HttpResponse.redirect("https://example.com")
    response.status shouldBe HttpStatus.Found_302
    response.isRedirection shouldBe true
    response.location shouldBe Some("https://example.com")
  }

  test("should create moved permanently response") {
    val response = HttpResponse.movedPermanently("https://example.com/new")
    response.status shouldBe HttpStatus.MovedPermanently_301
    response.location shouldBe Some("https://example.com/new")
  }

  test("should create see other response") {
    val response = HttpResponse.seeOther("https://example.com")
    response.status shouldBe HttpStatus.SeeOther_303
  }

  test("should create temporary redirect response") {
    val response = HttpResponse.temporaryRedirect("https://example.com/temp")
    response.status shouldBe HttpStatus.TemporaryRedirect_307
  }

  test("should create permanent redirect response") {
    val response = HttpResponse.permanentRedirect("https://example.com/new")
    response.status shouldBe HttpStatus.PermanentRedirect_308
  }

  test("should create not modified response") {
    val response = HttpResponse.notModified
    response.status shouldBe HttpStatus.NotModified_304
  }

  test("should create bad request response") {
    val response = HttpResponse.badRequest
    response.status shouldBe HttpStatus.BadRequest_400
    response.isClientError shouldBe true
  }

  test("should create bad request response with message") {
    val response = HttpResponse.badRequest("Invalid input")
    response.contentAsString shouldBe Some("Invalid input")
  }

  test("should create unauthorized response") {
    val response = HttpResponse.unauthorized
    response.status shouldBe HttpStatus.Unauthorized_401
  }

  test("should create forbidden response") {
    val response = HttpResponse.forbidden
    response.status shouldBe HttpStatus.Forbidden_403
  }

  test("should create not found response") {
    val response = HttpResponse.notFound
    response.status shouldBe HttpStatus.NotFound_404
    response.isClientError shouldBe true
  }

  test("should create method not allowed response") {
    val response = HttpResponse.methodNotAllowed
    response.status shouldBe HttpStatus.MethodNotAllowed_405
  }

  test("should create conflict response") {
    val response = HttpResponse.conflict
    response.status shouldBe HttpStatus.Conflict_409
  }

  test("should create gone response") {
    val response = HttpResponse.gone
    response.status shouldBe HttpStatus.Gone_410
  }

  test("should create unprocessable entity response") {
    val response = HttpResponse.unprocessableEntity
    response.status shouldBe HttpStatus.UnprocessableEntity_422
  }

  test("should create too many requests response") {
    val response = HttpResponse.tooManyRequests
    response.status shouldBe HttpStatus.TooManyRequests_429
  }

  test("should create internal server error response") {
    val response = HttpResponse.internalServerError
    response.status shouldBe HttpStatus.InternalServerError_500
    response.isServerError shouldBe true
  }

  test("should create not implemented response") {
    val response = HttpResponse.notImplemented
    response.status shouldBe HttpStatus.NotImplemented_501
  }

  test("should create bad gateway response") {
    val response = HttpResponse.badGateway
    response.status shouldBe HttpStatus.BadGateway_502
  }

  test("should create service unavailable response") {
    val response = HttpResponse.serviceUnavailable
    response.status shouldBe HttpStatus.ServiceUnavailable_503
  }

  test("should create gateway timeout response") {
    val response = HttpResponse.gatewayTimeout
    response.status shouldBe HttpStatus.GatewayTimeout_504
  }

  test("should classify response status") {
    HttpResponse.ok.isSuccessful shouldBe true
    HttpResponse.ok.isClientError shouldBe false
    HttpResponse.ok.isServerError shouldBe false
    HttpResponse.ok.isRedirection shouldBe false

    HttpResponse.notFound.isClientError shouldBe true
    HttpResponse.internalServerError.isServerError shouldBe true
    HttpResponse.redirect("/").isRedirection shouldBe true
  }

  test("should get reason phrase") {
    HttpResponse.ok.reason shouldBe "OK"
    HttpResponse.notFound.reason shouldBe "Not Found"
  }

  test("should add headers") {
    val response = HttpResponse.ok.addHeader("X-Custom", "value")
    response.headers.get("X-Custom") shouldBe Some("value")
  }

  test("should set headers") {
    val response = HttpResponse
      .ok
      .setHeader("Content-Type", "text/html")
      .setHeader("Content-Type", "application/json")
    response.headers.get("Content-Type") shouldBe Some("application/json")
  }

  test("should remove headers") {
    val response = HttpResponse.ok.addHeader("X-Custom", "value").removeHeader("X-Custom")
    response.headers.contains("X-Custom") shouldBe false
  }

  test("should add text content") {
    val response = HttpResponse.ok.withTextContent("Hello World")
    response.contentAsString shouldBe Some("Hello World")
  }

  test("should add JSON content as string") {
    val response = HttpResponse.ok.withJsonContent("""{"status": "ok"}""")
    response.contentAsString shouldBe Some("""{"status": "ok"}""")
  }

  test("should add bytes content") {
    val bytes    = "Hello".getBytes("UTF-8")
    val response = HttpResponse.ok.withBytesContent(bytes)
    response.contentAsBytes.isDefined shouldBe true
  }

  test("should add HTML content") {
    val response = HttpResponse.ok.withHtmlContent("<html></html>")
    response.contentAsString shouldBe Some("<html></html>")
    response.content.contentType.get.fullType shouldBe "text/html"
  }

  test("should set content type") {
    val response = HttpResponse.ok.withContentType(ContentType.ApplicationJson)
    response.headers.get("Content-Type") shouldBe Some("application/json")
  }

  test("should set location") {
    val response = HttpResponse.ok.withLocation("/new-location")
    response.location shouldBe Some("/new-location")
  }

  test("should get content type from content or headers") {
    val response1 = HttpResponse.ok.withJsonContent("""{}""")
    response1.contentType.isDefined shouldBe true
    response1.contentType.get.isJson shouldBe true

    val response2 = HttpResponse.ok.setHeader("Content-Type", "text/html")
    response2.contentType.isDefined shouldBe true
  }

  test("should copy with new status") {
    val response = HttpResponse.ok
    val modified = response.withStatus(HttpStatus.Created_201)
    modified.status shouldBe HttpStatus.Created_201
    response.status shouldBe HttpStatus.Ok_200 // original unchanged
  }

end HttpResponseTest
