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
  * Represents the body content of an HTTP message
  */
sealed trait HttpContent:
  def isEmpty: Boolean
  def nonEmpty: Boolean     = !isEmpty
  def contentType: Option[ContentType]
  def length: Long

  def asString: Option[String]
  def asBytes: Option[Array[Byte]]

object HttpContent:

  case object Empty extends HttpContent:
    def isEmpty: Boolean                = true
    def contentType: Option[ContentType] = None
    def length: Long                    = 0
    def asString: Option[String]        = None
    def asBytes: Option[Array[Byte]]    = None

  case class TextContent(
      text: String,
      override val contentType: Option[ContentType] = Some(ContentType.TextPlain)
  ) extends HttpContent:
    def isEmpty: Boolean             = text.isEmpty
    def length: Long                 = text.getBytes("UTF-8").length.toLong
    def asString: Option[String]     = Some(text)
    def asBytes: Option[Array[Byte]] = Some(text.getBytes("UTF-8"))

  case class ByteContent(
      bytes: Array[Byte],
      override val contentType: Option[ContentType] = Some(ContentType.ApplicationOctetStream)
  ) extends HttpContent:
    def isEmpty: Boolean             = bytes.isEmpty
    def length: Long                 = bytes.length.toLong
    def asString: Option[String]     = Some(String(bytes, "UTF-8"))
    def asBytes: Option[Array[Byte]] = Some(bytes)

  case class JsonContent(
      json: JSONValue,
      override val contentType: Option[ContentType] = Some(ContentType.ApplicationJson)
  ) extends HttpContent:
    def isEmpty: Boolean             = false
    def length: Long                 = json.toJSON.getBytes("UTF-8").length.toLong
    def asString: Option[String]     = Some(json.toJSON)
    def asBytes: Option[Array[Byte]] = Some(json.toJSON.getBytes("UTF-8"))

  def empty: HttpContent = Empty

  def text(s: String): HttpContent =
    if s.isEmpty then Empty else TextContent(s)

  def text(s: String, contentType: ContentType): HttpContent =
    if s.isEmpty then Empty else TextContent(s, Some(contentType))

  def bytes(b: Array[Byte]): HttpContent =
    if b.isEmpty then Empty else ByteContent(b)

  def bytes(b: Array[Byte], contentType: ContentType): HttpContent =
    if b.isEmpty then Empty else ByteContent(b, Some(contentType))

  def json(j: JSONValue): HttpContent =
    JsonContent(j)

  def json(s: String): HttpContent =
    TextContent(s, Some(ContentType.ApplicationJson))

  def html(s: String): HttpContent =
    TextContent(s, Some(ContentType.TextHtml))

  def xml(s: String): HttpContent =
    TextContent(s, Some(ContentType.ApplicationXml))
