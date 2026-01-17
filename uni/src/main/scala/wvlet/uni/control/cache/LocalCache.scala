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

import java.util.concurrent.atomic.AtomicLong
import scala.collection.mutable

/**
  * Internal entry that holds the cached value along with metadata.
  */
private[cache] class CacheEntry[K, V](
    val key: K,
    @volatile var value: V,
    @volatile var writeTimeNanos: Long,
    @volatile var accessTimeNanos: Long,
    val weight: Int
):
  // For doubly-linked list (LRU tracking)
  @volatile var prev: CacheEntry[K, V] = null
  @volatile var next: CacheEntry[K, V] = null

end CacheEntry

/**
  * Thread-safe LRU cache implementation with time-based expiration.
  *
  * Uses a hash map for O(1) lookups and a doubly-linked list for O(1) LRU operations.
  */
class LocalCache[K, V](
    val config: CacheConfig,
    weigher: Option[(K, V) => Int],
    removalListener: RemovalListener[K, V]
) extends Cache[K, V]:
  // Main storage
  private val data: mutable.HashMap[K, CacheEntry[K, V]] =
    mutable.HashMap.empty[K, CacheEntry[K, V]]

  // LRU doubly-linked list (head = most recently used, tail = least recently used)
  private var head: CacheEntry[K, V] = null
  private var tail: CacheEntry[K, V] = null
  private var currentSize: Long      = 0
  private var currentWeight: Long    = 0

  // Statistics counters
  private val _hitCount           = AtomicLong(0)
  private val _missCount          = AtomicLong(0)
  private val _loadSuccessCount   = AtomicLong(0)
  private val _loadFailureCount   = AtomicLong(0)
  private val _totalLoadTimeNanos = AtomicLong(0)
  private val _evictionCount      = AtomicLong(0)
  private val _evictionWeight     = AtomicLong(0)

  private def ticker: Ticker = config.ticker

  override def get(key: K): Option[V] = synchronized {
    cleanUpIfNeeded()
    data.get(key) match
      case Some(entry) if !isExpired(entry) =>
        recordHit()
        onAccess(entry)
        Some(entry.value)
      case Some(entry) =>
        // Expired entry
        recordMiss()
        removeEntry(entry, RemovalCause.Expired)
        None
      case None =>
        recordMiss()
        None
  }

  override def get(key: K, loader: K => V): V = synchronized {
    cleanUpIfNeeded()
    data.get(key) match
      case Some(entry) if !isExpired(entry) =>
        recordHit()
        onAccess(entry)
        entry.value
      case Some(entry) =>
        // Expired entry - remove and reload
        removeEntry(entry, RemovalCause.Expired)
        loadAndPut(key, loader)
      case None =>
        loadAndPut(key, loader)
  }

  private def loadAndPut(key: K, loader: K => V): V =
    recordMiss()
    val startNanos = ticker.read
    try
      val value         = loader(key)
      val loadTimeNanos = ticker.read - startNanos
      recordLoadSuccess(loadTimeNanos)
      putInternal(key, value)
      value
    catch
      case e: Throwable =>
        val loadTimeNanos = ticker.read - startNanos
        recordLoadFailure(loadTimeNanos)
        throw e

  override def put(key: K, value: V): Unit = synchronized {
    cleanUpIfNeeded()
    putInternal(key, value)
  }

  private def putInternal(key: K, value: V): Unit =
    val now    = ticker.read
    val weight = computeWeight(key, value)

    data.get(key) match
      case Some(existingEntry) =>
        // Replace existing entry
        val oldValue = existingEntry.value
        currentWeight -= existingEntry.weight
        existingEntry.value = value
        existingEntry.writeTimeNanos = now
        existingEntry.accessTimeNanos = now
        moveToHead(existingEntry)
        currentWeight += weight
        notifyRemoval(key, oldValue, RemovalCause.Replaced)
      case None =>
        // New entry
        val entry = CacheEntry[K, V](key, value, now, now, weight)
        data.put(key, entry)
        addToHead(entry)
        currentSize += 1
        currentWeight += weight
        evictIfNeeded()

  override def putIfAbsent(key: K, value: V): Option[V] = synchronized {
    cleanUpIfNeeded()
    data.get(key) match
      case Some(entry) if !isExpired(entry) =>
        onAccess(entry)
        Some(entry.value)
      case Some(entry) =>
        // Expired - remove and put new
        removeEntry(entry, RemovalCause.Expired)
        putInternal(key, value)
        None
      case None =>
        putInternal(key, value)
        None
  }

  override def putAll(entries: Map[K, V]): Unit = synchronized {
    entries.foreach { case (k, v) => putInternal(k, v) }
  }

  override def invalidate(key: K): Unit = synchronized {
    data.get(key).foreach { entry =>
      removeEntry(entry, RemovalCause.Explicit)
    }
  }

  override def invalidateAll(keys: Iterable[K]): Unit = synchronized {
    keys.foreach(invalidate)
  }

  override def invalidateAll(): Unit = synchronized {
    data.values.foreach { entry =>
      notifyRemoval(entry.key, entry.value, RemovalCause.Explicit)
    }
    data.clear()
    head = null
    tail = null
    currentSize = 0
    currentWeight = 0
  }

  override def asMap: Map[K, V] = synchronized {
    cleanUpIfNeeded()
    data.collect {
      case (k, entry) if !isExpired(entry) => k -> entry.value
    }.toMap
  }

  override def estimatedSize: Long = synchronized {
    currentSize
  }

  override def cleanUp(): Unit = synchronized {
    if config.hasExpiration then
      val expiredEntries = data.values.filter(isExpired).toList
      expiredEntries.foreach { entry =>
        removeEntry(entry, RemovalCause.Expired)
      }
  }

  override def stats: CacheStats = CacheStats(
    hitCount = _hitCount.get(),
    missCount = _missCount.get(),
    loadSuccessCount = _loadSuccessCount.get(),
    loadFailureCount = _loadFailureCount.get(),
    totalLoadTimeNanos = _totalLoadTimeNanos.get(),
    evictionCount = _evictionCount.get(),
    evictionWeight = _evictionWeight.get()
  )

  // --- Private helper methods ---

  private def computeWeight(key: K, value: V): Int =
    weigher match
      case Some(w) => w(key, value)
      case None    => 1

  private def isExpired(entry: CacheEntry[K, V]): Boolean =
    val now = ticker.read
    config.expireAfterWriteNanos.exists { ttl =>
      now - entry.writeTimeNanos > ttl
    } || config.expireAfterAccessNanos.exists { ttl =>
      now - entry.accessTimeNanos > ttl
    }

  private def onAccess(entry: CacheEntry[K, V]): Unit =
    entry.accessTimeNanos = ticker.read
    moveToHead(entry)

  private def evictIfNeeded(): Unit =
    // Evict based on max size
    config.maxSize.foreach { max =>
      while currentSize > max && tail != null do removeEntry(tail, RemovalCause.Size)
    }
    // Evict based on max weight
    config.maxWeight.foreach { max =>
      while currentWeight > max && tail != null do removeEntry(tail, RemovalCause.Size)
    }

  private def removeEntry(entry: CacheEntry[K, V], cause: RemovalCause): Unit =
    data.remove(entry.key)
    removeFromList(entry)
    currentSize -= 1
    currentWeight -= entry.weight
    if cause == RemovalCause.Size then
      _evictionCount.incrementAndGet()
      _evictionWeight.addAndGet(entry.weight)
    notifyRemoval(entry.key, entry.value, cause)

  private def notifyRemoval(key: K, value: V, cause: RemovalCause): Unit =
    val notification = RemovalNotification(key, value, cause)
    try removalListener.onRemoval(notification)
    catch case _: Throwable => () // Swallow exceptions from listeners

  // --- Doubly-linked list operations ---

  private def addToHead(entry: CacheEntry[K, V]): Unit =
    entry.prev = null
    entry.next = head
    if head != null then head.prev = entry
    head = entry
    if tail == null then tail = entry

  private def moveToHead(entry: CacheEntry[K, V]): Unit =
    if entry != head then
      removeFromList(entry)
      addToHead(entry)

  private def removeFromList(entry: CacheEntry[K, V]): Unit =
    if entry.prev != null then entry.prev.next = entry.next
    else head = entry.next

    if entry.next != null then entry.next.prev = entry.prev
    else tail = entry.prev

    entry.prev = null
    entry.next = null

  // --- Statistics recording ---

  private def recordHit(): Unit =
    if config.recordStats then _hitCount.incrementAndGet()

  private def recordMiss(): Unit =
    if config.recordStats then _missCount.incrementAndGet()

  private def recordLoadSuccess(loadTimeNanos: Long): Unit =
    if config.recordStats then
      _loadSuccessCount.incrementAndGet()
      _totalLoadTimeNanos.addAndGet(loadTimeNanos)

  private def recordLoadFailure(loadTimeNanos: Long): Unit =
    if config.recordStats then
      _loadFailureCount.incrementAndGet()
      _totalLoadTimeNanos.addAndGet(loadTimeNanos)

  // Periodic cleanup (only if expiration is configured)
  private var lastCleanupNanos: Long         = 0
  private val cleanupIntervalNanos: Long     = 1000000000L // 1 second

  private def cleanUpIfNeeded(): Unit =
    if config.hasExpiration then
      val now = ticker.read
      if now - lastCleanupNanos > cleanupIntervalNanos then
        lastCleanupNanos = now
        cleanUp()

end LocalCache

/**
  * LoadingCache implementation that automatically loads values.
  */
class LocalLoadingCache[K, V](
    config: CacheConfig,
    weigher: Option[(K, V) => Int],
    removalListener: RemovalListener[K, V],
    loader: K => V
) extends LocalCache[K, V](config, weigher, removalListener)
    with LoadingCache[K, V]:

  override def get(key: K): Option[V] =
    super.get(key, loader) match
      case v => Some(v)

  override def getAll(keys: Iterable[K]): Map[K, V] = synchronized {
    keys.map(key => key -> get(key, loader)).toMap
  }

  override def refresh(key: K): Unit = synchronized {
    try
      val value = loader(key)
      put(key, value)
    catch
      case e: Throwable =>
        // Keep existing value on refresh failure
        ()
  }

end LocalLoadingCache

object LocalCache:
  def apply[K, V](
      config: CacheConfig,
      weigher: Option[(K, V) => Int],
      removalListener: RemovalListener[K, V]
  ): LocalCache[K, V] =
    new LocalCache(config, weigher, removalListener)

object LocalLoadingCache:
  def apply[K, V](
      config: CacheConfig,
      weigher: Option[(K, V) => Int],
      removalListener: RemovalListener[K, V],
      loader: K => V
  ): LocalLoadingCache[K, V] =
    new LocalLoadingCache(config, weigher, removalListener, loader)
