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

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import scala.annotation.tailrec
import scala.collection.mutable.ArrayDeque

/**
  * A rate limiter that controls the rate of operations over time.
  *
  * Unlike RxSemaphore which limits concurrent operations, RateLimiter controls the rate at which
  * operations can be performed (e.g., 100 ops/sec).
  *
  * Supports multiple algorithms:
  *   - Token bucket: Smooth rate limiting with burst support (default)
  *   - Fixed window: Simple window-based counting
  *   - Sliding window: Precise rolling window rate limiting
  */
trait RateLimiter:

  /**
    * Acquire permission to proceed, blocking if rate limit is exceeded. Returns the wait time in
    * milliseconds (0 if no wait was needed).
    */
  def acquire(): Long

  /**
    * Acquire n permits, blocking if rate limit is exceeded. Returns the wait time in milliseconds.
    */
  def acquireN(n: Int): Long

  /**
    * Try to acquire without waiting. Returns true if permit was acquired, false if rate limit
    * exceeded.
    */
  def tryAcquire(): Boolean

  /**
    * Try to acquire n permits without waiting.
    */
  def tryAcquireN(n: Int): Boolean

  /**
    * Execute a block with rate limiting. Automatically waits if rate limit is exceeded.
    */
  def withLimit[A](body: => A): A

  /**
    * Execute a block with rate limiting, using n permits.
    */
  def withLimitN[A](n: Int)(body: => A): A

  /**
    * Get the current number of available permits.
    */
  def availablePermits: Double

  /**
    * Get the estimated wait time in milliseconds for acquiring a permit.
    */
  def estimatedWaitTimeMillis: Long

  /**
    * The configured rate (permits per second).
    */
  def ratePerSecond: Double

end RateLimiter

object RateLimiter:

  /**
    * Create a new builder for configuring a token bucket rate limiter.
    */
  def newBuilder: Builder = Builder()

  /**
    * A rate limiter that allows all operations (no limiting).
    */
  def unlimited: RateLimiter = UnlimitedRateLimiter

  /**
    * Create a fixed window rate limiter.
    *
    * Allows up to `maxOperations` within each `windowDuration`. The window resets at fixed
    * intervals.
    *
    * @param maxOperations
    *   maximum operations allowed per window
    * @param windowDuration
    *   the duration of each window
    * @param unit
    *   time unit for windowDuration
    */
  def fixedWindow(
      maxOperations: Int,
      windowDuration: Long,
      unit: TimeUnit,
      ticker: Ticker = Ticker.systemTicker
  ): RateLimiter = FixedWindowRateLimiter(maxOperations, unit.toNanos(windowDuration), ticker)

  /**
    * Create a sliding window rate limiter.
    *
    * Allows up to `maxOperations` within any rolling `windowDuration` period. More accurate than
    * fixed window but slightly more expensive.
    *
    * @param maxOperations
    *   maximum operations allowed in the window
    * @param windowDuration
    *   the sliding window duration
    * @param unit
    *   time unit for windowDuration
    */
  def slidingWindow(
      maxOperations: Int,
      windowDuration: Long,
      unit: TimeUnit,
      ticker: Ticker = Ticker.systemTicker
  ): RateLimiter = SlidingWindowRateLimiter(maxOperations, unit.toNanos(windowDuration), ticker)

  /**
    * Builder for token bucket RateLimiter with fluent configuration.
    */
  case class Builder(
      permitsPerSecond: Double = 1.0,
      burstSize: Int = 1,
      warmupPeriodMillis: Long = 0L,
      name: Option[String] = None,
      ticker: Ticker = Ticker.systemTicker
  ):
    require(permitsPerSecond > 0, s"permitsPerSecond must be positive: ${permitsPerSecond}")
    require(burstSize >= 1, s"burstSize must be at least 1: ${burstSize}")
    require(
      warmupPeriodMillis >= 0,
      s"warmupPeriodMillis must be non-negative: ${warmupPeriodMillis}"
    )

    /** Set the rate in permits per second */
    def withPermitsPerSecond(rate: Double): Builder = this.copy(permitsPerSecond = rate)

    /** Set the maximum burst size (bucket capacity) */
    def withBurstSize(size: Int): Builder = this.copy(burstSize = size)

    /** Set warmup period during which rate gradually increases */
    def withWarmupPeriod(millis: Long): Builder = this.copy(warmupPeriodMillis = millis)

    /** Set a name for this rate limiter (useful for logging/metrics) */
    def withName(n: String): Builder = this.copy(name = Some(n))

    /** Disable the name */
    def noName(): Builder = this.copy(name = None)

    /** Set a custom ticker (useful for testing) */
    def withTicker(t: Ticker): Builder = this.copy(ticker = t)

    /** Interval between permits in nanoseconds */
    def intervalNanos: Long = (1_000_000_000.0 / permitsPerSecond).toLong

    /** Build the rate limiter */
    def build(): RateLimiter = TokenBucketRateLimiter(this)

  end Builder

  // Internal state for the token bucket
  private case class State(storedPermits: Double, nextFreeTicketNanos: Long)

  /**
    * Token bucket rate limiter implementation (lock-free).
    */
  private class TokenBucketRateLimiter(config: Builder) extends RateLimiter:
    private val state = AtomicReference[State](State(config.burstSize.toDouble, config.ticker.read))

    override def ratePerSecond: Double = config.permitsPerSecond

    override def acquire(): Long = acquireN(1)

    override def acquireN(n: Int): Long =
      require(n >= 0, s"Cannot acquire negative permits: ${n}")
      if n == 0 then
        0L
      else
        val waitNanos = reserveAndGetWaitTime(n)
        if waitNanos > 0 then
          val waitMillis = TimeUnit.NANOSECONDS.toMillis(waitNanos)
          Thread.sleep(waitMillis)
          waitMillis
        else
          0L

    override def tryAcquire(): Boolean = tryAcquireN(1)

    override def tryAcquireN(n: Int): Boolean =
      require(n >= 0, s"Cannot acquire negative permits: ${n}")
      if n == 0 then
        true
      else
        tryReserve(n)

    override def withLimit[A](body: => A): A =
      acquire()
      body

    override def withLimitN[A](n: Int)(body: => A): A =
      acquireN(n)
      body

    override def availablePermits: Double =
      val current  = state.get()
      val nowNanos = config.ticker.read
      if nowNanos > current.nextFreeTicketNanos then
        val elapsedNanos = nowNanos - current.nextFreeTicketNanos
        val newPermits   = elapsedNanos.toDouble / config.intervalNanos
        (current.storedPermits + newPermits).min(config.burstSize.toDouble)
      else
        current.storedPermits

    override def estimatedWaitTimeMillis: Long =
      val current   = state.get()
      val nowNanos  = config.ticker.read
      val waitNanos = (current.nextFreeTicketNanos - nowNanos).max(0)
      TimeUnit.NANOSECONDS.toMillis(waitNanos)

    @tailrec
    private def reserveAndGetWaitTime(permits: Int): Long =
      val now     = config.ticker.read
      val current = state.get()

      // Refill tokens based on elapsed time
      val newStoredPermits =
        if now > current.nextFreeTicketNanos then
          val elapsedNanos = now - current.nextFreeTicketNanos
          val newPermits   = elapsedNanos.toDouble / config.intervalNanos
          (current.storedPermits + newPermits).min(config.burstSize.toDouble)
        else
          current.storedPermits

      // Calculate how many permits we can use from stored vs fresh
      val storedPermitsToUse = newStoredPermits.min(permits.toDouble)
      val freshPermits       = permits - storedPermitsToUse
      val waitNanos          = (freshPermits * config.intervalNanos).toLong

      val newNextFreeTicket =
        if now > current.nextFreeTicketNanos then
          now + waitNanos
        else
          current.nextFreeTicketNanos + waitNanos

      val newState = State(
        storedPermits = (newStoredPermits - storedPermitsToUse).max(0),
        nextFreeTicketNanos = newNextFreeTicket
      )

      if state.compareAndSet(current, newState) then
        // Return the wait time from now
        (current.nextFreeTicketNanos - now).max(0)
      else
        reserveAndGetWaitTime(permits)

    end reserveAndGetWaitTime

    @tailrec
    private def tryReserve(permits: Int): Boolean =
      val now     = config.ticker.read
      val current = state.get()

      // Can only succeed if nextFreeTicket is in the past
      if current.nextFreeTicketNanos > now then
        false
      else
        // Refill tokens
        val elapsedNanos     = now - current.nextFreeTicketNanos
        val newPermits       = elapsedNanos.toDouble / config.intervalNanos
        val newStoredPermits = (current.storedPermits + newPermits).min(config.burstSize.toDouble)

        if newStoredPermits < permits then
          false
        else
          val newState = State(
            storedPermits = newStoredPermits - permits,
            nextFreeTicketNanos = now
          )
          if state.compareAndSet(current, newState) then
            true
          else
            tryReserve(permits)

  end TokenBucketRateLimiter

  /**
    * Fixed window rate limiter implementation.
    */
  private class FixedWindowRateLimiter(maxOperations: Int, windowNanos: Long, ticker: Ticker)
      extends RateLimiter:
    private val lock         = ReentrantLock()
    private var windowStart  = ticker.read
    private var currentCount = 0L

    override def ratePerSecond: Double = maxOperations.toDouble * 1_000_000_000.0 / windowNanos

    override def tryAcquire(): Boolean = tryAcquireN(1)

    override def tryAcquireN(n: Int): Boolean =
      require(n >= 0, s"Cannot acquire negative permits: ${n}")
      if n == 0 then
        return true
      lock.lock()
      try
        val now = ticker.read
        if now - windowStart >= windowNanos then
          windowStart = now
          currentCount = n.toLong
          true
        else if currentCount + n <= maxOperations then
          currentCount += n
          true
        else
          false
      finally
        lock.unlock()

    override def acquire(): Long = acquireN(1)

    override def acquireN(n: Int): Long =
      require(n >= 0, s"Cannot acquire negative permits: ${n}")
      if n == 0 then
        return 0L
      val startTime = ticker.read
      while !tryAcquireN(n) do
        val sleepMillis =
          lock.lock()
          try
            val now = ticker.read
            if now - windowStart >= windowNanos then
              0L
            else
              val remainingNanos = windowNanos - (now - windowStart)
              (remainingNanos / 1000000).max(1)
          finally
            lock.unlock()
        if sleepMillis > 0 then
          Thread.sleep(sleepMillis)
      val endTime = ticker.read
      TimeUnit.NANOSECONDS.toMillis(endTime - startTime)

    override def withLimit[A](body: => A): A =
      acquire()
      body

    override def withLimitN[A](n: Int)(body: => A): A =
      acquireN(n)
      body

    override def availablePermits: Double =
      lock.lock()
      try
        val now = ticker.read
        if now - windowStart >= windowNanos then
          maxOperations.toDouble
        else
          (maxOperations - currentCount).max(0).toDouble
      finally
        lock.unlock()

    override def estimatedWaitTimeMillis: Long =
      lock.lock()
      try
        val now = ticker.read
        if now - windowStart >= windowNanos || currentCount < maxOperations then
          0L
        else
          val remainingNanos = windowNanos - (now - windowStart)
          TimeUnit.NANOSECONDS.toMillis(remainingNanos)
      finally
        lock.unlock()

  end FixedWindowRateLimiter

  /**
    * Sliding window rate limiter implementation.
    */
  private class SlidingWindowRateLimiter(maxOperations: Int, windowNanos: Long, ticker: Ticker)
      extends RateLimiter:
    private val lock       = ReentrantLock()
    private val timestamps = ArrayDeque[Long]()

    override def ratePerSecond: Double = maxOperations.toDouble * 1_000_000_000.0 / windowNanos

    private def cleanupOldEntries(now: Long): Unit =
      val cutoff = now - windowNanos
      while timestamps.nonEmpty && timestamps.head < cutoff do
        timestamps.removeHead()

    override def tryAcquire(): Boolean = tryAcquireN(1)

    override def tryAcquireN(n: Int): Boolean =
      require(n >= 0, s"Cannot acquire negative permits: ${n}")
      if n == 0 then
        return true
      lock.lock()
      try
        val now = ticker.read
        cleanupOldEntries(now)
        if timestamps.size + n <= maxOperations then
          for _ <- 1 to n do
            timestamps.addOne(now)
          true
        else
          false
      finally
        lock.unlock()

    override def acquire(): Long = acquireN(1)

    override def acquireN(n: Int): Long =
      require(n >= 0, s"Cannot acquire negative permits: ${n}")
      if n == 0 then
        return 0L
      val startTime = ticker.read
      while !tryAcquireN(n) do
        lock.lock()
        val sleepNanos =
          try
            val now = ticker.read
            cleanupOldEntries(now)
            if timestamps.nonEmpty then
              val oldestTimestamp = timestamps.head
              val waitUntil       = oldestTimestamp + windowNanos
              (waitUntil - now).max(1000000)
            else
              1000000
          finally
            lock.unlock()
        Thread.sleep(sleepNanos / 1000000, (sleepNanos % 1000000).toInt)
      val endTime = ticker.read
      TimeUnit.NANOSECONDS.toMillis(endTime - startTime)

    override def withLimit[A](body: => A): A =
      acquire()
      body

    override def withLimitN[A](n: Int)(body: => A): A =
      acquireN(n)
      body

    override def availablePermits: Double =
      lock.lock()
      try
        cleanupOldEntries(ticker.read)
        (maxOperations - timestamps.size).max(0).toDouble
      finally
        lock.unlock()

    override def estimatedWaitTimeMillis: Long =
      lock.lock()
      try
        val now = ticker.read
        cleanupOldEntries(now)
        if timestamps.size < maxOperations || timestamps.isEmpty then
          0L
        else
          val oldestTimestamp = timestamps.head
          val waitUntil       = oldestTimestamp + windowNanos
          TimeUnit.NANOSECONDS.toMillis((waitUntil - now).max(0))
      finally
        lock.unlock()

  end SlidingWindowRateLimiter

  /**
    * A rate limiter that allows all operations (no limiting).
    */
  private object UnlimitedRateLimiter extends RateLimiter:
    override def acquire(): Long                      = 0L
    override def acquireN(n: Int): Long               = 0L
    override def tryAcquire(): Boolean                = true
    override def tryAcquireN(n: Int): Boolean         = true
    override def withLimit[A](body: => A): A          = body
    override def withLimitN[A](n: Int)(body: => A): A = body
    override def availablePermits: Double             = Double.MaxValue
    override def estimatedWaitTimeMillis: Long        = 0L
    override def ratePerSecond: Double                = Double.MaxValue

end RateLimiter
