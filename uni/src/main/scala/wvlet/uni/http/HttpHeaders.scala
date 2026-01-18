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
  * A collection of HTTP headers that preserves insertion order and supports multiple values per header name
  */
case class HttpHeaders(entries: Seq[HttpHeader]):
  def get(name: String): Option[String] =
    entries.find(_.nameEquals(name)).map(_.value)

  def getAll(name: String): Seq[String] =
    entries.filter(_.nameEquals(name)).map(_.value)

  def getOrElse(name: String, default: => String): String =
    get(name).getOrElse(default)

  def contains(name: String): Boolean =
    entries.exists(_.nameEquals(name))

  def add(name: String, value: String): HttpHeaders =
    HttpHeaders(entries :+ HttpHeader(name, value))

  def addAll(headers: HttpHeaders): HttpHeaders =
    HttpHeaders(entries ++ headers.entries)

  def addAll(headers: Seq[(String, String)]): HttpHeaders =
    HttpHeaders(entries ++ headers.map { case (n, v) => HttpHeader(n, v) })

  def set(name: String, value: String): HttpHeaders =
    HttpHeaders(entries.filterNot(_.nameEquals(name)) :+ HttpHeader(name, value))

  def remove(name: String): HttpHeaders =
    HttpHeaders(entries.filterNot(_.nameEquals(name)))

  def contentType: Option[ContentType] =
    get(HttpHeader.ContentType).flatMap(ContentType.parse)

  def contentLength: Option[Long] =
    get(HttpHeader.ContentLength).flatMap(_.toLongOption)

  def host: Option[String]        = get(HttpHeader.Host)
  def userAgent: Option[String]   = get(HttpHeader.UserAgent)
  def authorization: Option[String] = get(HttpHeader.Authorization)
  def accept: Option[String]      = get(HttpHeader.Accept)

  def isEmpty: Boolean  = entries.isEmpty
  def nonEmpty: Boolean = entries.nonEmpty
  def size: Int         = entries.size

  def toSeq: Seq[(String, String)] =
    entries.map(h => (h.name, h.value))

  def toMap: Map[String, String] =
    entries.map(h => h.name -> h.value).toMap

  def toMultiMap: Map[String, Seq[String]] =
    entries.groupBy(_.name.toLowerCase).map { case (_, vs) =>
      vs.head.name -> vs.map(_.value)
    }

  override def toString: String =
    entries.map(_.toString).mkString("\r\n")

object HttpHeaders:
  val empty: HttpHeaders = HttpHeaders(Seq.empty)

  def apply(headers: (String, String)*): HttpHeaders =
    HttpHeaders(headers.map { case (n, v) => HttpHeader(n, v) })

  def of(headers: (String, String)*): HttpHeaders =
    apply(headers*)

  def fromMap(map: Map[String, String]): HttpHeaders =
    HttpHeaders(map.toSeq.map { case (n, v) => HttpHeader(n, v) })

  def fromSeq(headers: Seq[(String, String)]): HttpHeaders =
    HttpHeaders(headers.map { case (n, v) => HttpHeader(n, v) })
