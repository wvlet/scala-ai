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
package wvlet.uni.control.cache

import wvlet.uni.control.Ticker

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

/**
  * Immutable configuration and builder for Cache instances.
  *
  * Example usage:
  * {{{
  *   // Simple cache
  *   val cache = Cache.newBuilder
  *     .withMaximumSize(1000)
  *     .withExpirationAfterWrite(10, TimeUnit.MINUTES)
  *     .withStats
  *     .build[String, Int]()
  *
  *   // Loading cache with automatic value computation
  *   val loadingCache = Cache.newBuilder
  *     .withMaximumSize(100)
  *     .withExpirationAfterAccess(5, TimeUnit.MINUTES)
  *     .build((key: String) => computeValue(key))
  * }}}
  *
  * @param maxSize
  *   maximum number of entries (None for unlimited)
  * @param expireAfterWriteNanos
  *   expiration time after write in nanoseconds (None for no expiration)
  * @param expireAfterAccessNanos
  *   expiration time after access in nanoseconds (None for no expiration)
  * @param initialCapacity
  *   initial capacity for the underlying data structure
  * @param recordStats
  *   whether to record statistics
  * @param ticker
  *   ticker for time measurements (for testing)
  * @param maxWeight
  *   maximum total weight (None for size-based eviction)
  */
case class CacheConfig(
    maxSize: Option[Long] = None,
    expireAfterWriteNanos: Option[Long] = None,
    expireAfterAccessNanos: Option[Long] = None,
    refreshAfterWriteNanos: Option[Long] = None,
    initialCapacity: Int = 16,
    recordStats: Boolean = false,
    ticker: Ticker = Ticker.systemTicker,
    maxWeight: Option[Long] = None
):
  def hasExpiration: Boolean = expireAfterWriteNanos.isDefined || expireAfterAccessNanos.isDefined
  def hasRefresh: Boolean    = refreshAfterWriteNanos.isDefined
  def hasMaxSize: Boolean    = maxSize.isDefined
  def hasMaxWeight: Boolean  = maxWeight.isDefined

  // --- Builder methods ---

  /**
    * Sets the maximum number of entries the cache may contain.
    */
  def withMaximumSize(size: Long): CacheConfig =
    require(size >= 0, "maximumSize must not be negative")
    require(maxWeight.isEmpty, "maximumWeight already set")
    this.copy(maxSize = Some(size))

  /**
    * Sets the maximum total weight of entries the cache may contain.
    */
  def withMaximumWeight(weight: Long): CacheConfig =
    require(weight >= 0, "maximumWeight must not be negative")
    require(maxSize.isEmpty, "maximumSize already set")
    this.copy(maxWeight = Some(weight))

  /**
    * Specifies that entries should expire after a fixed duration since creation or last update.
    */
  def withExpirationAfterWrite(duration: Long, unit: TimeUnit): CacheConfig =
    require(duration >= 0, "duration must not be negative")
    this.copy(expireAfterWriteNanos = Some(unit.toNanos(duration)))

  /**
    * Specifies that entries should expire after a fixed duration since creation or last update.
    */
  def withExpirationAfterWrite(duration: Duration): CacheConfig = withExpirationAfterWrite(
    duration.toNanos,
    TimeUnit.NANOSECONDS
  )

  /**
    * Specifies that entries should expire after a fixed duration since last read or write.
    */
  def withExpirationAfterAccess(duration: Long, unit: TimeUnit): CacheConfig =
    require(duration >= 0, "duration must not be negative")
    this.copy(expireAfterAccessNanos = Some(unit.toNanos(duration)))

  /**
    * Specifies that entries should expire after a fixed duration since last read or write.
    */
  def withExpirationAfterAccess(duration: Duration): CacheConfig = withExpirationAfterAccess(
    duration.toNanos,
    TimeUnit.NANOSECONDS
  )

  /**
    * Specifies that entries should be refreshed after a fixed duration since creation or last
    * update. Unlike expiration, a stale entry will still be returned while a background refresh is
    * triggered. This is only effective for LoadingCache.
    *
    * Note: refreshAfterWrite should be less than expireAfterWrite to be useful.
    */
  def withRefreshAfterWrite(duration: Long, unit: TimeUnit): CacheConfig =
    require(duration >= 0, "duration must not be negative")
    this.copy(refreshAfterWriteNanos = Some(unit.toNanos(duration)))

  /**
    * Specifies that entries should be refreshed after a fixed duration since creation or last
    * update.
    */
  def withRefreshAfterWrite(duration: Duration): CacheConfig = withRefreshAfterWrite(
    duration.toNanos,
    TimeUnit.NANOSECONDS
  )

  /**
    * Sets the initial capacity of the cache.
    */
  def withInitialCapacity(capacity: Int): CacheConfig =
    require(capacity >= 0, "initialCapacity must not be negative")
    this.copy(initialCapacity = capacity)

  /**
    * Enables statistics recording.
    */
  def withStats: CacheConfig = this.copy(recordStats = true)

  /**
    * Disables statistics recording.
    */
  def noStats: CacheConfig = this.copy(recordStats = false)

  /**
    * Sets the ticker for time measurements. Use ManualTicker for testing.
    */
  def withTicker(ticker: Ticker): CacheConfig = this.copy(ticker = ticker)

  // --- Build methods ---

  /**
    * Builds a cache that does not automatically load values.
    */
  def build[K, V](): Cache[K, V] = LocalCache[K, V](this, None, RemovalListener.noop[K, V])

  /**
    * Builds a cache with a weigher function for weight-based eviction.
    */
  def build[K, V](weigher: (K, V) => Int): Cache[K, V] = LocalCache[K, V](
    this,
    Some(weigher),
    RemovalListener.noop[K, V]
  )

  /**
    * Builds a cache with a removal listener.
    */
  def buildWithRemovalListener[K, V](listener: RemovalListener[K, V]): Cache[K, V] =
    LocalCache[K, V](this, None, listener)

  /**
    * Builds a cache with a removal listener (from function).
    */
  def buildWithRemovalListener[K, V](f: RemovalNotification[K, V] => Unit): Cache[K, V] =
    LocalCache[K, V](this, None, RemovalListener(f))

  /**
    * Builds a cache with both weigher and removal listener.
    */
  def build[K, V](weigher: (K, V) => Int, listener: RemovalListener[K, V]): Cache[K, V] =
    LocalCache[K, V](this, Some(weigher), listener)

  /**
    * Builds a loading cache that automatically loads values using the given loader.
    */
  def build[K, V](loader: K => V): LoadingCache[K, V] = LocalLoadingCache[K, V](
    this,
    None,
    RemovalListener.noop[K, V],
    loader
  )

  /**
    * Builds a loading cache with a weigher function.
    */
  def build[K, V](loader: K => V, weigher: (K, V) => Int): LoadingCache[K, V] =
    LocalLoadingCache[K, V](this, Some(weigher), RemovalListener.noop[K, V], loader)

  /**
    * Builds a loading cache with a removal listener.
    */
  def buildWithRemovalListener[K, V](
      loader: K => V,
      listener: RemovalListener[K, V]
  ): LoadingCache[K, V] = LocalLoadingCache[K, V](this, None, listener, loader)

  /**
    * Builds a loading cache with a loader, weigher, and removal listener.
    */
  def build[K, V](
      loader: K => V,
      weigher: (K, V) => Int,
      listener: RemovalListener[K, V]
  ): LoadingCache[K, V] = LocalLoadingCache[K, V](this, Some(weigher), listener, loader)

end CacheConfig

object CacheConfig:
  val default: CacheConfig = CacheConfig()

end CacheConfig
