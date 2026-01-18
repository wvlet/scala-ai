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
  * Common trait for HTTP request and response messages
  */
trait HttpMessage[T <: HttpMessage[T]]:
  def headers: HttpHeaders
  def content: HttpContent

  def contentType: Option[ContentType] =
    content.contentType.orElse(headers.contentType)

  def contentLength: Option[Long] =
    headers.contentLength.orElse(Some(content.length))

  def header(name: String): Option[String]     = headers.get(name)
  def headerValues(name: String): Seq[String]  = headers.getAll(name)
  def hasHeader(name: String): Boolean         = headers.contains(name)
