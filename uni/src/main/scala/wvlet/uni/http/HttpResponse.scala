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

import wvlet.uni.json.{JSON, JSONValue}

/**
  * An immutable HTTP response representation
  */
case class HttpResponse(
    status: HttpStatus,
    headers: HttpHeaders = HttpHeaders.empty,
    content: HttpContent = HttpContent.Empty
) extends HttpMessage[HttpResponse]:

  def isSuccessful: Boolean   = status.isSuccessful
  def isClientError: Boolean  = status.isClientError
  def isServerError: Boolean  = status.isServerError
  def isRedirection: Boolean  = status.isRedirection
  def isInformational: Boolean = status.isInformational

  def statusCode: Int = status.code
  def reason: String  = status.reason

  def contentAsString: Option[String]     = content.asString
  def contentAsBytes: Option[Array[Byte]] = content.asBytes

  def contentAsJson: Option[JSONValue] = content match
    case HttpContent.JsonContent(json, _) => Some(json)
    case other                            => other.asString.map(JSON.parse)

  def location: Option[String] = headers.get(HttpHeader.Location)

  // Builder methods
  def withStatus(s: HttpStatus): HttpResponse    = copy(status = s)
  def withHeaders(h: HttpHeaders): HttpResponse  = copy(headers = h)
  def withContent(c: HttpContent): HttpResponse  = copy(content = c)

  def addHeader(name: String, value: String): HttpResponse =
    copy(headers = headers.add(name, value))

  def setHeader(name: String, value: String): HttpResponse =
    copy(headers = headers.set(name, value))

  def removeHeader(name: String): HttpResponse =
    copy(headers = headers.remove(name))

  def withTextContent(text: String): HttpResponse =
    copy(content = HttpContent.text(text))

  def withJsonContent(json: JSONValue): HttpResponse =
    copy(content = HttpContent.json(json))

  def withJsonContent(json: String): HttpResponse =
    copy(content = HttpContent.json(json))

  def withBytesContent(bytes: Array[Byte]): HttpResponse =
    copy(content = HttpContent.bytes(bytes))

  def withHtmlContent(html: String): HttpResponse =
    copy(content = HttpContent.html(html))

  def withContentType(ct: ContentType): HttpResponse =
    setHeader(HttpHeader.ContentType, ct.toString)

  def withLocation(uri: String): HttpResponse =
    setHeader(HttpHeader.Location, uri)

object HttpResponse:
  def apply(status: HttpStatus): HttpResponse =
    HttpResponse(status, HttpHeaders.empty, HttpContent.Empty)

  // 2xx Success responses
  def ok: HttpResponse                        = HttpResponse(HttpStatus.Ok_200)
  def ok(content: String): HttpResponse       = ok.withTextContent(content)
  def ok(content: HttpContent): HttpResponse  = ok.withContent(content)
  def created: HttpResponse                   = HttpResponse(HttpStatus.Created_201)
  def accepted: HttpResponse                  = HttpResponse(HttpStatus.Accepted_202)
  def noContent: HttpResponse                 = HttpResponse(HttpStatus.NoContent_204)

  // 3xx Redirection responses
  def redirect(location: String): HttpResponse =
    HttpResponse(HttpStatus.Found_302).withLocation(location)

  def movedPermanently(location: String): HttpResponse =
    HttpResponse(HttpStatus.MovedPermanently_301).withLocation(location)

  def seeOther(location: String): HttpResponse =
    HttpResponse(HttpStatus.SeeOther_303).withLocation(location)

  def temporaryRedirect(location: String): HttpResponse =
    HttpResponse(HttpStatus.TemporaryRedirect_307).withLocation(location)

  def permanentRedirect(location: String): HttpResponse =
    HttpResponse(HttpStatus.PermanentRedirect_308).withLocation(location)

  def notModified: HttpResponse = HttpResponse(HttpStatus.NotModified_304)

  // 4xx Client error responses
  def badRequest: HttpResponse                   = HttpResponse(HttpStatus.BadRequest_400)
  def badRequest(message: String): HttpResponse  = badRequest.withTextContent(message)
  def unauthorized: HttpResponse                 = HttpResponse(HttpStatus.Unauthorized_401)
  def forbidden: HttpResponse                    = HttpResponse(HttpStatus.Forbidden_403)
  def notFound: HttpResponse                     = HttpResponse(HttpStatus.NotFound_404)
  def notFound(message: String): HttpResponse    = notFound.withTextContent(message)
  def methodNotAllowed: HttpResponse             = HttpResponse(HttpStatus.MethodNotAllowed_405)
  def conflict: HttpResponse                     = HttpResponse(HttpStatus.Conflict_409)
  def gone: HttpResponse                         = HttpResponse(HttpStatus.Gone_410)
  def unprocessableEntity: HttpResponse          = HttpResponse(HttpStatus.UnprocessableEntity_422)
  def tooManyRequests: HttpResponse              = HttpResponse(HttpStatus.TooManyRequests_429)

  // 5xx Server error responses
  def internalServerError: HttpResponse              = HttpResponse(HttpStatus.InternalServerError_500)
  def internalServerError(message: String): HttpResponse = internalServerError.withTextContent(message)
  def notImplemented: HttpResponse                   = HttpResponse(HttpStatus.NotImplemented_501)
  def badGateway: HttpResponse                       = HttpResponse(HttpStatus.BadGateway_502)
  def serviceUnavailable: HttpResponse               = HttpResponse(HttpStatus.ServiceUnavailable_503)
  def gatewayTimeout: HttpResponse                   = HttpResponse(HttpStatus.GatewayTimeout_504)
