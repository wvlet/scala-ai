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
package wvlet.uni.control

import wvlet.uni.test.UniTest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class RateLimiterTest extends UniTest:

  test("fixed window rate limiter") {
    val limiter = RateLimiter.fixedWindow(3, 100, TimeUnit.MILLISECONDS)

    // Should allow first 3 operations
    limiter.tryAcquire() shouldBe true
    limiter.tryAcquire() shouldBe true
    limiter.tryAcquire() shouldBe true

    // Should reject 4th operation
    limiter.tryAcquire() shouldBe false

    // After window passes, should allow again
    Thread.sleep(120)
    limiter.tryAcquire() shouldBe true
  }

  test("sliding window rate limiter") {
    val limiter = RateLimiter.slidingWindow(3, 100, TimeUnit.MILLISECONDS)

    // Should allow first 3 operations
    limiter.tryAcquire() shouldBe true
    limiter.tryAcquire() shouldBe true
    limiter.tryAcquire() shouldBe true

    // Should reject 4th operation
    limiter.tryAcquire() shouldBe false

    // After window passes, should allow again
    Thread.sleep(120)
    limiter.tryAcquire() shouldBe true
  }

  test("token bucket rate limiter") {
    val limiter = RateLimiter.tokenBucket(
      maxTokens = 3,
      refillRate = 1,
      refillInterval = 50,
      unit = TimeUnit.MILLISECONDS
    )

    // Should allow first 3 operations (initial tokens)
    limiter.tryAcquire() shouldBe true
    limiter.tryAcquire() shouldBe true
    limiter.tryAcquire() shouldBe true

    // Should reject when bucket is empty
    limiter.tryAcquire() shouldBe false

    // After refill interval, should have 1 token
    Thread.sleep(60)
    limiter.tryAcquire() shouldBe true
    limiter.tryAcquire() shouldBe false
  }

  test("runOrDrop returns None when rate limited") {
    val limiter = RateLimiter.fixedWindow(1, 100, TimeUnit.MILLISECONDS)

    val first  = limiter.runOrDrop("success")
    val second = limiter.runOrDrop("dropped")

    first shouldBe Some("success")
    second shouldBe None
  }

  test("runBlocking blocks until permit available") {
    val limiter = RateLimiter.fixedWindow(1, 50, TimeUnit.MILLISECONDS)

    val counter   = new AtomicInteger(0)
    val startTime = System.currentTimeMillis()

    // First operation should be immediate
    limiter.runBlocking(counter.incrementAndGet())

    // Second operation should wait for window reset
    limiter.runBlocking(counter.incrementAndGet())

    val elapsed = System.currentTimeMillis() - startTime
    counter.get() shouldBe 2
    // Should have waited at least for the window duration
    (elapsed >= 40) shouldBe true
  }

  test("availablePermits returns correct count") {
    val limiter = RateLimiter.fixedWindow(5, 100, TimeUnit.MILLISECONDS)

    limiter.availablePermits shouldBe 5

    limiter.tryAcquire()
    limiter.tryAcquire()

    limiter.availablePermits shouldBe 3
  }

  test("perSecond convenience method") {
    val limiter = RateLimiter.perSecond(10)

    // Should allow initial burst
    for _ <- 1 to 10 do
      limiter.tryAcquire() shouldBe true

    // Should reject when exhausted
    limiter.tryAcquire() shouldBe false
  }

end RateLimiterTest
