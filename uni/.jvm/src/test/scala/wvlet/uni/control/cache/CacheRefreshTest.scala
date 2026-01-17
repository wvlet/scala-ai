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

import java.util.concurrent.TimeUnit

/**
  * JVM-only tests for refreshAfterWrite (requires Thread.sleep for timing).
  */
class CacheRefreshTest extends UniTest:

  test("refreshAfterWrite triggers background refresh") {
    val ticker    = Ticker.manualTicker
    var loadCount = 0
    val cache     = Cache
      .newBuilder
      .withRefreshAfterWrite(1, TimeUnit.MINUTES)
      .withTicker(ticker)
      .build((key: String) =>
        loadCount += 1
        s"${key}-v${loadCount}"
      )

    // Initial load
    cache.get("a") shouldBe Some("a-v1")
    loadCount shouldBe 1

    // Access before refresh period - should return cached value
    ticker.tick(TimeUnit.SECONDS.toNanos(30))
    cache.get("a") shouldBe Some("a-v1")
    loadCount shouldBe 1

    // Advance past refresh period
    ticker.tick(TimeUnit.SECONDS.toNanos(31))

    // Access returns stale value immediately, triggers background refresh
    cache.get("a") shouldBe Some("a-v1")

    // Wait for background refresh to complete
    Thread.sleep(100)

    // Now should have refreshed value
    cache.get("a") shouldBe Some("a-v2")
    loadCount shouldBe 2
  }

  test("refreshAfterWrite does not duplicate refresh") {
    val ticker    = Ticker.manualTicker
    var loadCount = 0
    val cache     = Cache
      .newBuilder
      .withRefreshAfterWrite(1, TimeUnit.MINUTES)
      .withTicker(ticker)
      .build((key: String) =>
        loadCount += 1
        Thread.sleep(50) // Simulate slow load
        s"${key}-v${loadCount}"
      )

    // Initial load
    cache.get("a")
    loadCount shouldBe 1

    // Advance past refresh period
    ticker.tick(TimeUnit.MINUTES.toNanos(2))

    // Multiple rapid accesses should only trigger one refresh
    cache.get("a")
    cache.get("a")
    cache.get("a")

    // Wait for refresh to complete
    Thread.sleep(150)

    // Should only have loaded twice (initial + one refresh)
    loadCount shouldBe 2
  }

end CacheRefreshTest
