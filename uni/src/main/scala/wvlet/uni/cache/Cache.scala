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
package wvlet.uni.cache

import java.util.concurrent.TimeUnit
import scala.collection.mutable

/**
  * A high-performance, pure-Scala in-memory cache similar to Caffeine.
  *
  * Features:
  *   - Time-based expiration (after write, after access)
  *   - Size-based eviction with LRU policy
  *   - Statistics tracking (hit rate, miss rate, evictions)
  *   - Removal listeners for cleanup callbacks
  *   - Thread-safe operations
  *
  * @tparam K
  *   the type of keys
  * @tparam V
  *   the type of values
  */
trait Cache[K, V]:
  /**
    * Returns the value associated with the key, or None if not present or expired.
    */
  def get(key: K): Option[V]

  /**
    * Returns the value associated with the key, computing it if absent.
    */
  def get(key: K, loader: K => V): V

  /**
    * Associates the value with the key.
    */
  def put(key: K, value: V): Unit

  /**
    * Associates the value with the key only if not already present.
    * @return
    *   the previous value if present, otherwise None
    */
  def putIfAbsent(key: K, value: V): Option[V]

  /**
    * Copies all entries from the map into the cache.
    */
  def putAll(entries: Map[K, V]): Unit

  /**
    * Discards the entry for the key.
    */
  def invalidate(key: K): Unit

  /**
    * Discards all entries for the given keys.
    */
  def invalidateAll(keys: Iterable[K]): Unit

  /**
    * Discards all entries in the cache.
    */
  def invalidateAll(): Unit

  /**
    * Returns an unmodifiable snapshot of the cache as a Map.
    */
  def asMap: Map[K, V]

  /**
    * Returns the approximate number of entries in the cache.
    */
  def estimatedSize: Long

  /**
    * Performs any pending maintenance operations (e.g., eviction of expired entries).
    */
  def cleanUp(): Unit

  /**
    * Returns the cache statistics.
    */
  def stats: CacheStats

end Cache

object Cache:
  /**
    * Returns a new cache builder with default settings.
    *
    * Example usage:
    * {{{
    *   val cache = Cache.newBuilder
    *     .withMaximumSize(1000)
    *     .withExpirationAfterWrite(10, TimeUnit.MINUTES)
    *     .build[String, Int]()
    * }}}
    */
  def newBuilder: CacheConfig = CacheConfig.default

end Cache

/**
  * A cache that automatically loads values when they are not present.
  *
  * @tparam K
  *   the type of keys
  * @tparam V
  *   the type of values
  */
trait LoadingCache[K, V] extends Cache[K, V]:
  /**
    * Returns the value associated with the key, loading it if necessary.
    */
  def get(key: K): Option[V]

  /**
    * Returns a map of values for the given keys, loading any that are absent.
    */
  def getAll(keys: Iterable[K]): Map[K, V]

  /**
    * Loads a new value for the key, regardless of whether a value is already cached.
    */
  def refresh(key: K): Unit

end LoadingCache

/**
  * Reason why an entry was removed from the cache.
  */
enum RemovalCause:
  /** The entry was manually removed by the user. */
  case Explicit
  /** The entry was replaced by a new value. */
  case Replaced
  /** The entry was removed due to size constraints. */
  case Size
  /** The entry expired after write. */
  case Expired
  /** The entry expired after access. */
  case ExpiredAfterAccess

/**
  * Notification of an entry removal from cache.
  *
  * @param key
  *   the key that was removed
  * @param value
  *   the value that was removed
  * @param cause
  *   the reason for removal
  */
case class RemovalNotification[K, V](key: K, value: V, cause: RemovalCause):
  def wasEvicted: Boolean = cause == RemovalCause.Size || cause == RemovalCause.Expired ||
    cause == RemovalCause.ExpiredAfterAccess

/**
  * Listener for cache entry removals.
  */
trait RemovalListener[K, V]:
  def onRemoval(notification: RemovalNotification[K, V]): Unit

object RemovalListener:
  /**
    * Creates a removal listener from a function.
    */
  def apply[K, V](f: RemovalNotification[K, V] => Unit): RemovalListener[K, V] =
    new RemovalListener[K, V]:
      override def onRemoval(notification: RemovalNotification[K, V]): Unit = f(notification)

  /**
    * A no-op removal listener.
    */
  def noop[K, V]: RemovalListener[K, V] =
    new RemovalListener[K, V]:
      override def onRemoval(notification: RemovalNotification[K, V]): Unit = ()

end RemovalListener

/**
  * Statistics about cache operations.
  */
case class CacheStats(
    hitCount: Long,
    missCount: Long,
    loadSuccessCount: Long,
    loadFailureCount: Long,
    totalLoadTimeNanos: Long,
    evictionCount: Long,
    evictionWeight: Long
):
  /**
    * The total number of requests.
    */
  def requestCount: Long = hitCount + missCount

  /**
    * The ratio of cache hits to requests, or 1.0 if no requests.
    */
  def hitRate: Double =
    if requestCount == 0 then 1.0
    else hitCount.toDouble / requestCount

  /**
    * The ratio of cache misses to requests, or 0.0 if no requests.
    */
  def missRate: Double =
    if requestCount == 0 then 0.0
    else missCount.toDouble / requestCount

  /**
    * The total number of load operations.
    */
  def loadCount: Long = loadSuccessCount + loadFailureCount

  /**
    * The ratio of load failures to loads, or 0.0 if no loads.
    */
  def loadFailureRate: Double =
    if loadCount == 0 then 0.0
    else loadFailureCount.toDouble / loadCount

  /**
    * The average time spent loading values, in nanoseconds.
    */
  def averageLoadPenaltyNanos: Double =
    if loadCount == 0 then 0.0
    else totalLoadTimeNanos.toDouble / loadCount

  /**
    * Returns a new CacheStats that is the difference between this and another.
    */
  def minus(other: CacheStats): CacheStats = CacheStats(
    hitCount = this.hitCount - other.hitCount,
    missCount = this.missCount - other.missCount,
    loadSuccessCount = this.loadSuccessCount - other.loadSuccessCount,
    loadFailureCount = this.loadFailureCount - other.loadFailureCount,
    totalLoadTimeNanos = this.totalLoadTimeNanos - other.totalLoadTimeNanos,
    evictionCount = this.evictionCount - other.evictionCount,
    evictionWeight = this.evictionWeight - other.evictionWeight
  )

  /**
    * Returns a new CacheStats that is the sum of this and another.
    */
  def plus(other: CacheStats): CacheStats = CacheStats(
    hitCount = this.hitCount + other.hitCount,
    missCount = this.missCount + other.missCount,
    loadSuccessCount = this.loadSuccessCount + other.loadSuccessCount,
    loadFailureCount = this.loadFailureCount + other.loadFailureCount,
    totalLoadTimeNanos = this.totalLoadTimeNanos + other.totalLoadTimeNanos,
    evictionCount = this.evictionCount + other.evictionCount,
    evictionWeight = this.evictionWeight + other.evictionWeight
  )

  override def toString: String =
    s"CacheStats(hitCount=${hitCount}, missCount=${missCount}, hitRate=${f"${hitRate}%.2f"}, " +
      s"loadSuccessCount=${loadSuccessCount}, loadFailureCount=${loadFailureCount}, " +
      s"evictionCount=${evictionCount})"

end CacheStats

object CacheStats:
  val empty: CacheStats = CacheStats(
    hitCount = 0,
    missCount = 0,
    loadSuccessCount = 0,
    loadFailureCount = 0,
    totalLoadTimeNanos = 0,
    evictionCount = 0,
    evictionWeight = 0
  )

end CacheStats
