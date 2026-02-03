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
package wvlet.uni.dom

import org.scalajs.dom
import wvlet.uni.rx.{Cancelable, RxVar}

import scala.scalajs.js

/**
  * Type class for serializing/deserializing storage values.
  */
trait StorageCodec[A]:
  def encode(value: A): String
  def decode(raw: String): Option[A]

object StorageCodec:
  given stringCodec: StorageCodec[String] with
    def encode(value: String): String       = value
    def decode(raw: String): Option[String] = Some(raw)

  given intCodec: StorageCodec[Int] with
    def encode(value: Int): String       = value.toString
    def decode(raw: String): Option[Int] = raw.toIntOption

  given longCodec: StorageCodec[Long] with
    def encode(value: Long): String       = value.toString
    def decode(raw: String): Option[Long] = raw.toLongOption

  given doubleCodec: StorageCodec[Double] with
    def encode(value: Double): String       = value.toString
    def decode(raw: String): Option[Double] = raw.toDoubleOption

  given booleanCodec: StorageCodec[Boolean] with
    def encode(value: Boolean): String       = value.toString
    def decode(raw: String): Option[Boolean] = raw.toBooleanOption

/**
  * A reactive variable backed by browser storage (localStorage or sessionStorage).
  *
  * Changes are automatically persisted to storage and synchronized across browser tabs via storage
  * events.
  */
class StorageVar[A](key: String, default: A, storage: dom.Storage, codec: StorageCodec[A])
    extends RxVar[A](StorageVar.loadInitial(key, default, storage, codec)):

  private val storageListener: js.Function1[dom.StorageEvent, Unit] =
    (e: dom.StorageEvent) =>
      if e.key == key && e.storageArea == storage then
        val newValue = Option(e.newValue).flatMap(codec.decode).getOrElse(default)
        super.update(_ => newValue, force = true)

  // Listen for storage events from other tabs
  dom.window.addEventListener("storage", storageListener)

  override def update(updater: A => A, force: Boolean = false): Unit =
    super.update(updater, force)
    storage.setItem(key, codec.encode(get))

  /**
    * Remove the key from storage and reset to default value.
    */
  def clear(): Unit =
    storage.removeItem(key)
    super.update(_ => default, force = true)

  /**
    * Stop listening for storage events.
    */
  def dispose(): Unit = dom.window.removeEventListener("storage", storageListener)

end StorageVar

object StorageVar:
  private def loadInitial[A](
      key: String,
      default: A,
      storage: dom.Storage,
      codec: StorageCodec[A]
  ): A = Option(storage.getItem(key)).flatMap(codec.decode).getOrElse(default)

/**
  * Reactive localStorage and sessionStorage bindings.
  *
  * Usage:
  * {{{
  *   // Persistent theme preference
  *   val theme = Storage.local("theme", "light")
  *   html(
  *     cls -> theme,
  *     button(onclick -> { () => theme := "dark" }, "Dark Mode")
  *   )
  *
  *   // Session-only data
  *   val sessionToken = Storage.session("token", "")
  * }}}
  */
object Storage:
  /**
    * Create a reactive variable backed by localStorage.
    *
    * The value is automatically persisted when changed and synchronized across browser tabs.
    *
    * @param key
    *   The localStorage key
    * @param default
    *   Default value if the key doesn't exist
    */
  def local[A](key: String, default: A)(using codec: StorageCodec[A]): StorageVar[A] = StorageVar(
    key,
    default,
    dom.window.localStorage,
    codec
  )

  /**
    * Create a reactive variable backed by sessionStorage.
    *
    * The value is persisted only for the duration of the browser session.
    *
    * @param key
    *   The sessionStorage key
    * @param default
    *   Default value if the key doesn't exist
    */
  def session[A](key: String, default: A)(using codec: StorageCodec[A]): StorageVar[A] = StorageVar(
    key,
    default,
    dom.window.sessionStorage,
    codec
  )

  /**
    * Remove a key from localStorage.
    */
  def removeLocal(key: String): Unit = dom.window.localStorage.removeItem(key)

  /**
    * Remove a key from sessionStorage.
    */
  def removeSession(key: String): Unit = dom.window.sessionStorage.removeItem(key)

  /**
    * Clear all localStorage.
    */
  def clearLocal(): Unit = dom.window.localStorage.clear()

  /**
    * Clear all sessionStorage.
    */
  def clearSession(): Unit = dom.window.sessionStorage.clear()

end Storage
