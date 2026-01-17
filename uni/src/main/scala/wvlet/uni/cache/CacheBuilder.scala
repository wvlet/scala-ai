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

import wvlet.uni.control.Ticker

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

/**
  * Configuration for cache behavior.
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
  * @param removalListener
  *   listener for removal notifications
  * @param weigher
  *   function to compute the weight of an entry
  * @param maxWeight
  *   maximum total weight (None for size-based eviction)
  */
case class CacheConfig[K, V](
    maxSize: Option[Long] = None,
    expireAfterWriteNanos: Option[Long] = None,
    expireAfterAccessNanos: Option[Long] = None,
    initialCapacity: Int = 16,
    recordStats: Boolean = false,
    ticker: Ticker = Ticker.systemTicker,
    removalListener: RemovalListener[K, V] = RemovalListener.noop[K, V],
    weigher: Option[(K, V) => Int] = None,
    maxWeight: Option[Long] = None
):
  def hasExpiration: Boolean = expireAfterWriteNanos.isDefined || expireAfterAccessNanos.isDefined
  def hasMaxSize: Boolean    = maxSize.isDefined
  def hasMaxWeight: Boolean  = maxWeight.isDefined && weigher.isDefined

end CacheConfig

/**
  * Builder for creating Cache instances with fluent API.
  *
  * Example usage:
  * {{{
  *   val cache = CacheBuilder[String, Int]()
  *     .maximumSize(1000)
  *     .expireAfterWrite(10, TimeUnit.MINUTES)
  *     .recordStats()
  *     .build()
  *
  *   val loadingCache = CacheBuilder[String, Int]()
  *     .maximumSize(1000)
  *     .expireAfterAccess(5, TimeUnit.MINUTES)
  *     .build(key => computeValue(key))
  * }}}
  *
  * @tparam K
  *   the type of keys
  * @tparam V
  *   the type of values
  */
case class CacheBuilder[K, V](config: CacheConfig[K, V] = CacheConfig[K, V]()):

  /**
    * Sets the maximum number of entries the cache may contain.
    */
  def maximumSize(size: Long): CacheBuilder[K, V] =
    require(size >= 0, "maximumSize must not be negative")
    require(config.maxWeight.isEmpty, "maximumWeight already set")
    this.copy(config = config.copy(maxSize = Some(size)))

  /**
    * Sets the maximum total weight of entries the cache may contain.
    */
  def maximumWeight(weight: Long): CacheBuilder[K, V] =
    require(weight >= 0, "maximumWeight must not be negative")
    require(config.maxSize.isEmpty, "maximumSize already set")
    this.copy(config = config.copy(maxWeight = Some(weight)))

  /**
    * Sets the weigher function for computing entry weights.
    */
  def weigher(weigher: (K, V) => Int): CacheBuilder[K, V] =
    this.copy(config = config.copy(weigher = Some(weigher)))

  /**
    * Specifies that entries should expire after a fixed duration since creation or last update.
    */
  def expireAfterWrite(duration: Long, unit: TimeUnit): CacheBuilder[K, V] =
    require(duration >= 0, "duration must not be negative")
    this.copy(config = config.copy(expireAfterWriteNanos = Some(unit.toNanos(duration))))

  /**
    * Specifies that entries should expire after a fixed duration since creation or last update.
    */
  def expireAfterWrite(duration: Duration): CacheBuilder[K, V] =
    expireAfterWrite(duration.toNanos, TimeUnit.NANOSECONDS)

  /**
    * Specifies that entries should expire after a fixed duration since last read or write.
    */
  def expireAfterAccess(duration: Long, unit: TimeUnit): CacheBuilder[K, V] =
    require(duration >= 0, "duration must not be negative")
    this.copy(config = config.copy(expireAfterAccessNanos = Some(unit.toNanos(duration))))

  /**
    * Specifies that entries should expire after a fixed duration since last read or write.
    */
  def expireAfterAccess(duration: Duration): CacheBuilder[K, V] =
    expireAfterAccess(duration.toNanos, TimeUnit.NANOSECONDS)

  /**
    * Sets the initial capacity of the cache.
    */
  def initialCapacity(capacity: Int): CacheBuilder[K, V] =
    require(capacity >= 0, "initialCapacity must not be negative")
    this.copy(config = config.copy(initialCapacity = capacity))

  /**
    * Enables statistics recording.
    */
  def recordStats(): CacheBuilder[K, V] =
    this.copy(config = config.copy(recordStats = true))

  /**
    * Disables statistics recording.
    */
  def noRecordStats(): CacheBuilder[K, V] =
    this.copy(config = config.copy(recordStats = false))

  /**
    * Sets the ticker for time measurements. Use ManualTicker for testing.
    */
  def withTicker(ticker: Ticker): CacheBuilder[K, V] =
    this.copy(config = config.copy(ticker = ticker))

  /**
    * Sets the removal listener.
    */
  def removalListener(listener: RemovalListener[K, V]): CacheBuilder[K, V] =
    this.copy(config = config.copy(removalListener = listener))

  /**
    * Sets the removal listener from a function.
    */
  def removalListener(f: RemovalNotification[K, V] => Unit): CacheBuilder[K, V] =
    removalListener(RemovalListener(f))

  /**
    * Builds a cache that does not automatically load values.
    */
  def build(): Cache[K, V] = LocalCache[K, V](config)

  /**
    * Builds a cache that automatically loads values using the given loader.
    */
  def build(loader: K => V): LoadingCache[K, V] = LocalLoadingCache[K, V](config, loader)

end CacheBuilder

object CacheBuilder:
  /**
    * Creates a new CacheBuilder.
    */
  def apply[K, V](): CacheBuilder[K, V] = CacheBuilder[K, V](CacheConfig[K, V]())

  /**
    * Creates a simple cache with default settings.
    */
  def newCache[K, V](): Cache[K, V] = CacheBuilder[K, V]().build()

  /**
    * Creates a simple cache with maximum size.
    */
  def newCache[K, V](maxSize: Long): Cache[K, V] =
    CacheBuilder[K, V]().maximumSize(maxSize).build()

end CacheBuilder
