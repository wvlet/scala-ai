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
import scala.annotation.tailrec

/**
  * A rate limiter that controls the rate of operations over time.
  *
  * Unlike RxSemaphore which limits concurrent operations, RateLimiter controls the rate at which
  * operations can be performed (e.g., 100 ops/sec).
  *
  * The implementation uses the Token Bucket algorithm for smooth rate limiting with burst support.
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
    * Create a new builder for configuring a rate limiter.
    */
  def newBuilder: Builder = Builder()

  /**
    * A rate limiter that allows all operations (no limiting).
    */
  def unlimited: RateLimiter = UnlimitedRateLimiter

  /**
    * Builder for RateLimiter with fluent configuration.
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
    def withPermitsPerSecond(rate: Double): Builder =
      this.copy(permitsPerSecond = rate)

    /** Set the maximum burst size (bucket capacity) */
    def withBurstSize(size: Int): Builder =
      this.copy(burstSize = size)

    /** Set warmup period during which rate gradually increases */
    def withWarmupPeriod(millis: Long): Builder =
      this.copy(warmupPeriodMillis = millis)

    /** Set a name for this rate limiter (useful for logging/metrics) */
    def withName(n: String): Builder =
      this.copy(name = Some(n))

    /** Disable the name */
    def noName(): Builder =
      this.copy(name = None)

    /** Set a custom ticker (useful for testing) */
    def withTicker(t: Ticker): Builder =
      this.copy(ticker = t)

    /** Interval between permits in nanoseconds */
    def intervalNanos: Long = (1_000_000_000.0 / permitsPerSecond).toLong

    /** Build the rate limiter */
    def build(): RateLimiter =
      new TokenBucketRateLimiter(this)

  end Builder

  /**
    * Internal state for the token bucket
    */
  private case class State(storedPermits: Double, nextFreeTicketNanos: Long)

  /**
    * Token bucket rate limiter implementation.
    */
  private class TokenBucketRateLimiter(config: Builder) extends RateLimiter:
    private val state = new AtomicReference[State](
      State(config.burstSize.toDouble, config.ticker.read)
    )

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
    * A rate limiter that allows all operations (no limiting).
    */
  private object UnlimitedRateLimiter extends RateLimiter:
    override def acquire(): Long                    = 0L
    override def acquireN(n: Int): Long             = 0L
    override def tryAcquire(): Boolean              = true
    override def tryAcquireN(n: Int): Boolean       = true
    override def withLimit[A](body: => A): A        = body
    override def withLimitN[A](n: Int)(body: => A): A = body
    override def availablePermits: Double           = Double.MaxValue
    override def estimatedWaitTimeMillis: Long      = 0L
    override def ratePerSecond: Double              = Double.MaxValue

end RateLimiter
