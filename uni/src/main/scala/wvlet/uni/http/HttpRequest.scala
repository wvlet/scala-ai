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

import wvlet.uni.json.JSONValue

/**
  * An immutable HTTP request representation
  */
case class HttpRequest(
    method: HttpMethod,
    uri: String,
    headers: HttpHeaders = HttpHeaders.empty,
    content: HttpContent = HttpContent.Empty,
    queryParams: Map[String, Seq[String]] = Map.empty
) extends HttpMessage[HttpRequest]:

  def path: String =
    val idx = uri.indexOf('?')
    if idx >= 0 then uri.substring(0, idx) else uri

  def query: Option[String] =
    val idx = uri.indexOf('?')
    if idx >= 0 then Some(uri.substring(idx + 1)) else None

  def host: Option[String]      = headers.host
  def userAgent: Option[String] = headers.userAgent

  def getQueryParam(name: String): Option[String] =
    queryParams.get(name).flatMap(_.headOption)

  def getQueryParams(name: String): Seq[String] =
    queryParams.getOrElse(name, Seq.empty)

  def fullUri: String =
    if queryParams.isEmpty then uri
    else
      val queryString = queryParams
        .flatMap { case (k, vs) => vs.map(v => s"${k}=${v}") }
        .mkString("&")
      val separator = if uri.contains("?") then "&" else "?"
      s"${uri}${separator}${queryString}"

  // Builder methods
  def withMethod(m: HttpMethod): HttpRequest                 = copy(method = m)
  def withUri(u: String): HttpRequest                        = copy(uri = u)
  def withHeaders(h: HttpHeaders): HttpRequest               = copy(headers = h)
  def withContent(c: HttpContent): HttpRequest               = copy(content = c)
  def withQueryParams(params: Map[String, Seq[String]]): HttpRequest = copy(queryParams = params)

  def addHeader(name: String, value: String): HttpRequest =
    copy(headers = headers.add(name, value))

  def setHeader(name: String, value: String): HttpRequest =
    copy(headers = headers.set(name, value))

  def removeHeader(name: String): HttpRequest =
    copy(headers = headers.remove(name))

  def addQueryParam(name: String, value: String): HttpRequest =
    val existing = queryParams.getOrElse(name, Seq.empty)
    copy(queryParams = queryParams + (name -> (existing :+ value)))

  def setQueryParam(name: String, value: String): HttpRequest =
    copy(queryParams = queryParams + (name -> Seq(value)))

  def setQueryParam(name: String, values: Seq[String]): HttpRequest =
    copy(queryParams = queryParams + (name -> values))

  def withTextContent(text: String): HttpRequest =
    copy(content = HttpContent.text(text))

  def withJsonContent(json: JSONValue): HttpRequest =
    copy(content = HttpContent.json(json))

  def withJsonContent(json: String): HttpRequest =
    copy(content = HttpContent.json(json))

  def withBytesContent(bytes: Array[Byte]): HttpRequest =
    copy(content = HttpContent.bytes(bytes))

  def withContentType(ct: ContentType): HttpRequest =
    setHeader(HttpHeader.ContentType, ct.toString)

  def withHost(host: String): HttpRequest =
    setHeader(HttpHeader.Host, host)

  def withUserAgent(ua: String): HttpRequest =
    setHeader(HttpHeader.UserAgent, ua)

  def withAccept(accept: String): HttpRequest =
    setHeader(HttpHeader.Accept, accept)

  def withAuthorization(auth: String): HttpRequest =
    setHeader(HttpHeader.Authorization, auth)

  def withBearerToken(token: String): HttpRequest =
    withAuthorization(s"Bearer ${token}")

  def withBasicAuth(username: String, password: String): HttpRequest =
    val encoded = java.util.Base64.getEncoder.encodeToString(s"${username}:${password}".getBytes("UTF-8"))
    withAuthorization(s"Basic ${encoded}")

object HttpRequest:
  def apply(method: HttpMethod, uri: String): HttpRequest =
    HttpRequest(method, uri, HttpHeaders.empty, HttpContent.Empty, Map.empty)

  def get(uri: String): HttpRequest     = HttpRequest(HttpMethod.GET, uri)
  def post(uri: String): HttpRequest    = HttpRequest(HttpMethod.POST, uri)
  def put(uri: String): HttpRequest     = HttpRequest(HttpMethod.PUT, uri)
  def delete(uri: String): HttpRequest  = HttpRequest(HttpMethod.DELETE, uri)
  def patch(uri: String): HttpRequest   = HttpRequest(HttpMethod.PATCH, uri)
  def head(uri: String): HttpRequest    = HttpRequest(HttpMethod.HEAD, uri)
  def options(uri: String): HttpRequest = HttpRequest(HttpMethod.OPTIONS, uri)
