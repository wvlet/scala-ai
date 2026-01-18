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
  * An immutable, case-insensitive multi-map for HTTP headers and query parameters. Supports
  * multiple values per key while providing case-insensitive key lookup.
  */
class HttpMultiMap private (
    private val underlying: Map[String, Seq[String]],
    private val originalKeys: Map[String, String]
):
  def isEmpty: Boolean  = underlying.isEmpty
  def nonEmpty: Boolean = underlying.nonEmpty
  def size: Int         = underlying.size

  /**
    * Get the first value for a key (case-insensitive)
    */
  def get(key: String): Option[String] = underlying.get(key.toLowerCase).flatMap(_.headOption)

  /**
    * Get all values for a key (case-insensitive)
    */
  def getAll(key: String): Seq[String] = underlying.getOrElse(key.toLowerCase, Seq.empty)

  /**
    * Get the first value or a default
    */
  def getOrElse(key: String, default: => String): String = get(key).getOrElse(default)

  /**
    * Check if a key exists (case-insensitive)
    */
  def contains(key: String): Boolean = underlying.contains(key.toLowerCase)

  /**
    * Add a value to a key, preserving existing values
    */
  def add(key: String, value: String): HttpMultiMap =
    val lowerKey     = key.toLowerCase
    val existingVals = underlying.getOrElse(lowerKey, Seq.empty)
    val newOrigKeys  =
      if originalKeys.contains(lowerKey) then
        originalKeys
      else
        originalKeys + (lowerKey -> key)
    HttpMultiMap.create(underlying + (lowerKey -> (existingVals :+ value)), newOrigKeys)

  /**
    * Set a key to a single value, replacing any existing values
    */
  def set(key: String, value: String): HttpMultiMap =
    val lowerKey    = key.toLowerCase
    val newOrigKeys =
      if originalKeys.contains(lowerKey) then
        originalKeys
      else
        originalKeys + (lowerKey -> key)
    HttpMultiMap.create(underlying + (lowerKey -> Seq(value)), newOrigKeys)

  /**
    * Remove all values for a key (case-insensitive)
    */
  def remove(key: String): HttpMultiMap =
    val lowerKey = key.toLowerCase
    HttpMultiMap.create(underlying - lowerKey, originalKeys - lowerKey)

  /**
    * Add a key-value pair (alias for add)
    */
  def +(entry: (String, String)): HttpMultiMap = add(entry._1, entry._2)

  /**
    * Add all entries from another map
    */
  def ++(other: HttpMultiMap): HttpMultiMap =
    other
      .entries
      .foldLeft(this) { case (acc, (k, v)) =>
        acc.add(k, v)
      }

  /**
    * Get all entries as a sequence of (key, value) pairs. Keys use original casing.
    */
  def entries: Seq[(String, String)] = underlying
    .toSeq
    .flatMap { case (lowerKey, values) =>
      val origKey = originalKeys.getOrElse(lowerKey, lowerKey)
      values.map(v => origKey -> v)
    }

  /**
    * Get all keys (in original casing)
    */
  def keys: Iterable[String] = originalKeys.values

  /**
    * Convert to a raw Map (lowercase keys)
    */
  def toMap: Map[String, Seq[String]] = underlying

  override def toString: String = entries
    .map { case (k, v) =>
      s"${k}: ${v}"
    }
    .mkString(", ")

end HttpMultiMap

object HttpMultiMap:
  private[http] def create(
      underlying: Map[String, Seq[String]],
      originalKeys: Map[String, String]
  ): HttpMultiMap = new HttpMultiMap(underlying, originalKeys)

  val empty: HttpMultiMap = create(Map.empty, Map.empty)

  def apply(entries: (String, String)*): HttpMultiMap =
    entries.foldLeft(empty) { case (acc, (k, v)) =>
      acc.add(k, v)
    }

  def fromSeq(entries: Seq[(String, String)]): HttpMultiMap =
    entries.foldLeft(empty) { case (acc, (k, v)) =>
      acc.add(k, v)
    }

  def fromMap(map: Map[String, String]): HttpMultiMap =
    map.foldLeft(empty) { case (acc, (k, v)) =>
      acc.add(k, v)
    }

  def fromMultiMap(map: Map[String, Seq[String]]): HttpMultiMap =
    map.foldLeft(empty) { case (acc, (k, vs)) =>
      vs.foldLeft(acc)((a, v) => a.add(k, v))
    }

  class Builder:
    private var map: HttpMultiMap = empty

    def add(key: String, value: String): Builder =
      map = map.add(key, value)
      this

    def +=(entry: (String, String)): Builder = add(entry._1, entry._2)

    def ++=(entries: Seq[(String, String)]): Builder =
      entries.foreach(e => add(e._1, e._2))
      this

    def result(): HttpMultiMap = map

  def newBuilder: Builder = Builder()

end HttpMultiMap
