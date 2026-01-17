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
import wvlet.uni.test.UniTest
import wvlet.uni.test.empty
import wvlet.uni.test.defined

import java.util.concurrent.TimeUnit
import scala.collection.mutable.ArrayBuffer

class CacheTest extends UniTest:

  test("basic get and put") {
    val cache = Cache.newBuilder.build[String, Int]()

    cache.get("a") shouldBe empty
    cache.put("a", 1)
    cache.get("a") shouldBe Some(1)

    cache.put("a", 2)
    cache.get("a") shouldBe Some(2)
  }

  test("putIfAbsent") {
    val cache = Cache.newBuilder.build[String, Int]()

    cache.putIfAbsent("a", 1) shouldBe empty
    cache.get("a") shouldBe Some(1)

    cache.putIfAbsent("a", 2) shouldBe Some(1)
    cache.get("a") shouldBe Some(1)
  }

  test("putAll") {
    val cache = Cache.newBuilder.build[String, Int]()

    cache.putAll(Map("a" -> 1, "b" -> 2, "c" -> 3))
    cache.get("a") shouldBe Some(1)
    cache.get("b") shouldBe Some(2)
    cache.get("c") shouldBe Some(3)
  }

  test("get with loader") {
    var loadCount = 0
    val cache     = Cache.newBuilder.build[String, Int]()

    val result = cache.get(
      "a",
      key =>
        loadCount += 1
        key.length
    )
    result shouldBe 1
    loadCount shouldBe 1

    // Second get should use cached value
    val result2 = cache.get(
      "a",
      key =>
        loadCount += 1
        key.length * 10
    )
    result2 shouldBe 1
    loadCount shouldBe 1 // Loader not called again
  }

  test("invalidate") {
    val cache = Cache.newBuilder.build[String, Int]()

    cache.put("a", 1)
    cache.put("b", 2)
    cache.get("a") shouldBe Some(1)

    cache.invalidate("a")
    cache.get("a") shouldBe empty
    cache.get("b") shouldBe Some(2)
  }

  test("invalidateAll with keys") {
    val cache = Cache.newBuilder.build[String, Int]()

    cache.putAll(Map("a" -> 1, "b" -> 2, "c" -> 3))
    cache.invalidateAll(Seq("a", "c"))

    cache.get("a") shouldBe empty
    cache.get("b") shouldBe Some(2)
    cache.get("c") shouldBe empty
  }

  test("invalidateAll") {
    val cache = Cache.newBuilder.build[String, Int]()

    cache.putAll(Map("a" -> 1, "b" -> 2, "c" -> 3))
    cache.estimatedSize shouldBe 3

    cache.invalidateAll()
    cache.estimatedSize shouldBe 0
    cache.asMap shouldBe Map.empty
  }

  test("asMap") {
    val cache = Cache.newBuilder.build[String, Int]()

    cache.putAll(Map("a" -> 1, "b" -> 2))
    val map = cache.asMap
    map shouldBe Map("a" -> 1, "b" -> 2)
  }

  test("maximumSize with LRU eviction") {
    val cache = Cache.newBuilder.withMaximumSize(3).build[String, Int]()

    cache.put("a", 1)
    cache.put("b", 2)
    cache.put("c", 3)
    cache.estimatedSize shouldBe 3

    // Adding fourth element should evict "a" (LRU)
    cache.put("d", 4)
    cache.estimatedSize shouldBe 3
    cache.get("a") shouldBe empty
    cache.get("b") shouldBe Some(2)
    cache.get("c") shouldBe Some(3)
    cache.get("d") shouldBe Some(4)
  }

  test("LRU updates on access") {
    val cache = Cache.newBuilder.withMaximumSize(3).build[String, Int]()

    cache.put("a", 1)
    cache.put("b", 2)
    cache.put("c", 3)

    // Access "a" to make it most recently used
    cache.get("a")

    // Now "b" should be evicted (least recently used)
    cache.put("d", 4)
    cache.get("a") shouldBe Some(1)
    cache.get("b") shouldBe empty
    cache.get("c") shouldBe Some(3)
    cache.get("d") shouldBe Some(4)
  }

  test("expireAfterWrite") {
    val ticker = Ticker.manualTicker
    val cache  = Cache
      .newBuilder
      .withExpirationAfterWrite(1, TimeUnit.MINUTES)
      .withTicker(ticker)
      .build[String, Int]()

    cache.put("a", 1)
    cache.get("a") shouldBe Some(1)

    // Advance time but not enough to expire
    ticker.tick(TimeUnit.SECONDS.toNanos(30))
    cache.get("a") shouldBe Some(1)

    // Advance time past expiration
    ticker.tick(TimeUnit.SECONDS.toNanos(31))
    cache.get("a") shouldBe empty
  }

  test("expireAfterAccess") {
    val ticker = Ticker.manualTicker
    val cache  = Cache
      .newBuilder
      .withExpirationAfterAccess(1, TimeUnit.MINUTES)
      .withTicker(ticker)
      .build[String, Int]()

    cache.put("a", 1)
    cache.get("a") shouldBe Some(1)

    // Advance time but access before expiration
    ticker.tick(TimeUnit.SECONDS.toNanos(30))
    cache.get("a") shouldBe Some(1) // This resets the access time

    // Advance time but access resets timer
    ticker.tick(TimeUnit.SECONDS.toNanos(30))
    cache.get("a") shouldBe Some(1) // Still valid

    // Now let it expire
    ticker.tick(TimeUnit.SECONDS.toNanos(61))
    cache.get("a") shouldBe empty
  }

  test("recordStats") {
    val cache = Cache.newBuilder.withStats.build[String, Int]()

    cache.get("a") // miss
    cache.get("a") // miss
    cache.put("a", 1)
    cache.get("a") // hit
    cache.get("a") // hit
    cache.get("b") // miss

    val stats = cache.stats
    stats.hitCount shouldBe 2
    stats.missCount shouldBe 3
    stats.hitRate shouldBe 0.4
    stats.missRate shouldBe 0.6
  }

  test("stats for loader") {
    val ticker = Ticker.manualTicker
    val cache  = Cache.newBuilder.withStats.withTicker(ticker).build[String, Int]()

    cache.get(
      "a",
      _ =>
        ticker.tick(1000) // Simulate 1000 nanos load time
        42
    )
    cache.get("a", _ => 100) // Should use cached value

    val stats = cache.stats
    stats.loadSuccessCount shouldBe 1
    stats.totalLoadTimeNanos shouldBe 1000
    stats.hitCount shouldBe 1
    stats.missCount shouldBe 1
  }

  test("eviction stats") {
    val cache = Cache.newBuilder.withMaximumSize(2).withStats.build[String, Int]()

    cache.put("a", 1)
    cache.put("b", 2)
    cache.put("c", 3) // Evicts "a"

    val stats = cache.stats
    stats.evictionCount shouldBe 1
    stats.evictionWeight shouldBe 1
  }

  test("removalListener") {
    val notifications = ArrayBuffer.empty[RemovalNotification[String, Int]]
    val cache         = Cache
      .newBuilder
      .withMaximumSize(2)
      .buildWithRemovalListener[String, Int](n => notifications += n)

    cache.put("a", 1)
    cache.put("b", 2)
    cache.put("c", 3) // Should evict "a"

    (notifications.size >= 1) shouldBe true
    notifications.exists(n => n.key == "a" && n.cause == RemovalCause.Size) shouldBe true

    notifications.clear()
    cache.invalidate("b")
    (notifications.size >= 1) shouldBe true
    notifications.exists(n => n.key == "b" && n.cause == RemovalCause.Explicit) shouldBe true
  }

  test("replacement notifies listener") {
    val notifications = ArrayBuffer.empty[RemovalNotification[String, Int]]
    val cache = Cache.newBuilder.buildWithRemovalListener[String, Int](n => notifications += n)

    cache.put("a", 1)
    cache.put("a", 2) // Replace

    notifications.exists(n =>
      n.key == "a" && n.value == 1 && n.cause == RemovalCause.Replaced
    ) shouldBe true
  }

  test("weigher with maximumWeight") {
    val cache = Cache.newBuilder.withMaximumWeight(10).build[String, String]((_, v) => v.length)

    cache.put("a", "hello") // weight = 5
    cache.put("b", "hi")    // weight = 2, total = 7
    cache.estimatedSize shouldBe 2

    cache.put("c", "world") // weight = 5, total = 12 > 10, should evict
    cache.estimatedSize shouldBe 2
    cache.get("a") shouldBe empty // "a" was evicted (LRU)
    cache.get("b") shouldBe Some("hi")
    cache.get("c") shouldBe Some("world")
  }

  test("cleanUp removes expired entries") {
    val ticker = Ticker.manualTicker
    val cache  = Cache
      .newBuilder
      .withExpirationAfterWrite(1, TimeUnit.MINUTES)
      .withTicker(ticker)
      .build[String, Int]()

    cache.put("a", 1)
    cache.put("b", 2)
    cache.estimatedSize shouldBe 2

    ticker.tick(TimeUnit.MINUTES.toNanos(2))
    cache.cleanUp()
    cache.estimatedSize shouldBe 0
  }

  test("CacheStats arithmetic") {
    val stats1 = CacheStats(10, 5, 3, 1, 1000, 2, 2)
    val stats2 = CacheStats(20, 10, 6, 2, 2000, 4, 4)

    val sum = stats1.plus(stats2)
    sum.hitCount shouldBe 30
    sum.missCount shouldBe 15
    sum.loadSuccessCount shouldBe 9
    sum.loadFailureCount shouldBe 3
    sum.totalLoadTimeNanos shouldBe 3000
    sum.evictionCount shouldBe 6
    sum.evictionWeight shouldBe 6

    val diff = stats2.minus(stats1)
    diff.hitCount shouldBe 10
    diff.missCount shouldBe 5
    diff.loadSuccessCount shouldBe 3
  }

  test("CacheStats computed values") {
    val stats = CacheStats(80, 20, 15, 5, 100000, 10, 10)

    stats.requestCount shouldBe 100
    stats.hitRate shouldBe 0.8
    stats.missRate shouldBe 0.2
    stats.loadCount shouldBe 20
    stats.loadFailureRate shouldBe 0.25
    stats.averageLoadPenaltyNanos shouldBe 5000.0
  }

  test("CacheStats empty") {
    val stats = CacheStats.empty
    stats.hitRate shouldBe 1.0 // No requests = 100% hit rate
    stats.missRate shouldBe 0.0
    stats.loadFailureRate shouldBe 0.0
    stats.averageLoadPenaltyNanos shouldBe 0.0
  }

  test("RemovalNotification wasEvicted") {
    val explicit           = RemovalNotification("a", 1, RemovalCause.Explicit)
    val replaced           = RemovalNotification("a", 1, RemovalCause.Replaced)
    val size               = RemovalNotification("a", 1, RemovalCause.Size)
    val expiredAfterWrite  = RemovalNotification("a", 1, RemovalCause.ExpiredAfterWrite)
    val expiredAfterAccess = RemovalNotification("a", 1, RemovalCause.ExpiredAfterAccess)

    explicit.wasEvicted shouldBe false
    replaced.wasEvicted shouldBe false
    size.wasEvicted shouldBe true
    expiredAfterWrite.wasEvicted shouldBe true
    expiredAfterAccess.wasEvicted shouldBe true
  }

  test("removalListener for expireAfterWrite") {
    val ticker        = Ticker.manualTicker
    val notifications = ArrayBuffer.empty[RemovalNotification[String, Int]]
    val cache         = Cache
      .newBuilder
      .withExpirationAfterWrite(1, TimeUnit.MINUTES)
      .withTicker(ticker)
      .buildWithRemovalListener[String, Int](n => notifications += n)

    cache.put("a", 1)
    ticker.tick(TimeUnit.MINUTES.toNanos(2))
    cache.get("a") // Triggers removal of expired entry

    notifications.size shouldBe 1
    val n = notifications.head
    n.key shouldBe "a"
    n.cause shouldBe RemovalCause.ExpiredAfterWrite
  }

  test("removalListener for expireAfterAccess") {
    val ticker        = Ticker.manualTicker
    val notifications = ArrayBuffer.empty[RemovalNotification[String, Int]]
    val cache         = Cache
      .newBuilder
      .withExpirationAfterAccess(1, TimeUnit.MINUTES)
      .withTicker(ticker)
      .buildWithRemovalListener[String, Int](n => notifications += n)

    cache.put("a", 1)
    ticker.tick(TimeUnit.MINUTES.toNanos(2))
    cache.get("a") // Triggers removal of expired entry

    notifications.size shouldBe 1
    val n = notifications.head
    n.key shouldBe "a"
    n.cause shouldBe RemovalCause.ExpiredAfterAccess
  }

end CacheTest

class LoadingCacheTest extends UniTest:

  test("automatic loading") {
    var loadCount = 0
    val cache     = Cache
      .newBuilder
      .build((key: String) =>
        loadCount += 1
        key.length
      )

    cache.get("hello") shouldBe Some(5)
    loadCount shouldBe 1

    cache.get("hello") shouldBe Some(5)
    loadCount shouldBe 1 // Not loaded again

    cache.get("hi") shouldBe Some(2)
    loadCount shouldBe 2
  }

  test("getAll") {
    var loadCount = 0
    val cache     = Cache
      .newBuilder
      .build((key: String) =>
        loadCount += 1
        key.length
      )

    cache.put("hello", 5) // Pre-populate

    val result = cache.getAll(Seq("hello", "world", "hi"))
    result shouldBe Map("hello" -> 5, "world" -> 5, "hi" -> 2)
    loadCount shouldBe 2 // "hello" was cached, "world" and "hi" were loaded
  }

  test("refresh") {
    var counter = 0
    val cache   = Cache
      .newBuilder
      .build((_: String) =>
        counter += 1
        counter
      )

    cache.get("a") shouldBe Some(1)
    cache.get("a") shouldBe Some(1) // Still cached

    cache.refresh("a")
    cache.get("a") shouldBe Some(2) // Refreshed
  }

  test("loading with expiration") {
    val ticker    = Ticker.manualTicker
    var loadCount = 0
    val cache     = Cache
      .newBuilder
      .withExpirationAfterWrite(1, TimeUnit.MINUTES)
      .withTicker(ticker)
      .build((key: String) =>
        loadCount += 1
        key.length
      )

    cache.get("hello") shouldBe Some(5)
    loadCount shouldBe 1

    // Value is cached
    cache.get("hello") shouldBe Some(5)
    loadCount shouldBe 1

    // Expire the value
    ticker.tick(TimeUnit.MINUTES.toNanos(2))

    // Should reload
    cache.get("hello") shouldBe Some(5)
    loadCount shouldBe 2
  }

  test("loading with size limit") {
    var loadCount = 0
    val cache     = Cache
      .newBuilder
      .withMaximumSize(2)
      .build((key: String) =>
        loadCount += 1
        key.length
      )

    cache.get("a")
    cache.get("b")
    cache.get("c") // Evicts "a"
    loadCount shouldBe 3

    cache.get("a") // Reloads "a"
    loadCount shouldBe 4
  }

  test("reject null from loader") {
    val cache = Cache.newBuilder.build((_: String) => null.asInstanceOf[String])

    // Should throw NullPointerException when loader returns null
    intercept[NullPointerException] {
      cache.get("a")
    }
  }

  test("refresh ignores null from loader") {
    var returnNull = false
    val cache      = Cache
      .newBuilder
      .build((key: String) =>
        if returnNull then
          null.asInstanceOf[String]
        else
          key.toUpperCase
      )

    cache.get("hello") shouldBe Some("HELLO")

    // Refresh with null should keep existing value
    returnNull = true
    cache.refresh("hello")
    cache.get("hello") shouldBe Some("HELLO") // Still the old value
  }

  // Note: refreshAfterWrite tests are in CacheRefreshTest (JVM-only) due to Thread.sleep usage

end LoadingCacheTest

class CacheConfigTest extends UniTest:

  test("builder validation") {
    // maximumSize must be non-negative
    intercept[IllegalArgumentException] {
      Cache.newBuilder.withMaximumSize(-1)
    }

    // maximumWeight must be non-negative
    intercept[IllegalArgumentException] {
      Cache.newBuilder.withMaximumWeight(-1)
    }

    // Cannot set both maximumSize and maximumWeight
    intercept[IllegalArgumentException] {
      Cache.newBuilder.withMaximumSize(100).withMaximumWeight(100)
    }

    // expireAfterWrite must be non-negative
    intercept[IllegalArgumentException] {
      Cache.newBuilder.withExpirationAfterWrite(-1, TimeUnit.SECONDS)
    }

    // expireAfterAccess must be non-negative
    intercept[IllegalArgumentException] {
      Cache.newBuilder.withExpirationAfterAccess(-1, TimeUnit.SECONDS)
    }

    // initialCapacity must be non-negative
    intercept[IllegalArgumentException] {
      Cache.newBuilder.withInitialCapacity(-1)
    }
  }

  test("builder chaining") {
    val cache = Cache
      .newBuilder
      .withMaximumSize(100)
      .withExpirationAfterWrite(1, TimeUnit.HOURS)
      .withExpirationAfterAccess(30, TimeUnit.MINUTES)
      .withInitialCapacity(16)
      .withStats
      .build[String, Int]()

    cache.put("a", 1)
    cache.get("a") shouldBe Some(1)
  }

  test("noStats") {
    val cache = Cache.newBuilder.withStats.noStats.build[String, Int]()

    cache.get("a")
    cache.put("a", 1)
    cache.get("a")

    // Stats should not be recorded
    val stats = cache.stats
    stats.hitCount shouldBe 0
    stats.missCount shouldBe 0
  }

end CacheConfigTest
