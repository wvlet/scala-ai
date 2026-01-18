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
  * HTTP request methods as defined in RFC 9110
  */
enum HttpMethod(val name: String):
  case GET     extends HttpMethod("GET")
  case POST    extends HttpMethod("POST")
  case PUT     extends HttpMethod("PUT")
  case DELETE  extends HttpMethod("DELETE")
  case PATCH   extends HttpMethod("PATCH")
  case HEAD    extends HttpMethod("HEAD")
  case OPTIONS extends HttpMethod("OPTIONS")
  case TRACE   extends HttpMethod("TRACE")
  case CONNECT extends HttpMethod("CONNECT")

  def isSafe: Boolean =
    this match
      case GET | HEAD | OPTIONS | TRACE =>
        true
      case _ =>
        false

  def isIdempotent: Boolean =
    this match
      case POST =>
        false
      case _ =>
        true

  override def toString: String = name

object HttpMethod:
  def of(name: String): Option[HttpMethod] = values.find(_.name.equalsIgnoreCase(name))

  def unapply(name: String): Option[HttpMethod] = of(name)
