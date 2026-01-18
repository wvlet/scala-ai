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

/**
  * Handler for processing HTTP requests on the server side
  */
trait HttpHandler:
  def handle(request: HttpRequest): HttpResponse

object HttpHandler:
  def apply(f: HttpRequest => HttpResponse): HttpHandler = (request: HttpRequest) => f(request)

  val notFound: HttpHandler = _ => HttpResponse.notFound

/**
  * Middleware for transforming HTTP requests and responses
  */
trait HttpFilter:
  def apply(request: HttpRequest, next: HttpHandler): HttpResponse

  def andThen(other: HttpFilter): HttpFilter =
    val self = this
    (request: HttpRequest, next: HttpHandler) =>
      self.apply(
        request,
        (req: HttpRequest) => other.apply(req, next)
      )

object HttpFilter:
  val identity: HttpFilter = (request, next) => next.handle(request)

  def apply(f: (HttpRequest, HttpHandler) => HttpResponse): HttpFilter =
    (request: HttpRequest, next: HttpHandler) => f(request, next)

  def chain(filters: Seq[HttpFilter]): HttpFilter =
    filters.foldRight(identity) { (filter, acc) => filter.andThen(acc) }

  def chain(filters: HttpFilter*): HttpFilter =
    chain(filters.toSeq)

/**
  * Context for handling HTTP requests with additional metadata
  */
case class HttpContext(
    request: HttpRequest,
    attributes: Map[String, Any] = Map.empty
):
  def get[T](key: String): Option[T] =
    attributes.get(key).map(_.asInstanceOf[T])

  def getOrElse[T](key: String, default: => T): T =
    get[T](key).getOrElse(default)

  def set(key: String, value: Any): HttpContext =
    copy(attributes = attributes + (key -> value))

  def remove(key: String): HttpContext =
    copy(attributes = attributes - key)

object HttpContext:
  def apply(request: HttpRequest): HttpContext = HttpContext(request, Map.empty)
