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
import java.util.concurrent.locks.ReentrantLock
import scala.collection.mutable.ArrayDeque

/**
  * Rate limiter for controlling the rate of operations.
  *
  * Supports multiple algorithms:
  *   - Fixed window: limits operations within fixed time windows
  *   - Sliding window: limits operations within a rolling time window
  *   - Token bucket: tokens are replenished at a fixed rate, allows bursts
  *
  * Inspired by Ox's RateLimiter: https://github.com/softwaremill/ox
  */
trait RateLimiter:

  /**
    * Run the operation, blocking if the rate limit is reached until a permit is available.
    */
  def runBlocking[A](operation: => A): A

  /**
    * Run the operation if a permit is available, or return None if rate limited.
    */
  def runOrDrop[A](operation: => A): Option[A]

  /**
    * Try to acquire a permit without blocking.
    * @return
    *   true if permit was acquired, false otherwise
    */
  def tryAcquire(): Boolean

  /**
    * Acquire a permit, blocking until one is available.
    */
  def acquire(): Unit

  /**
    * Get the current number of available permits (approximate for some algorithms).
    */
  def availablePermits: Int

end RateLimiter

object RateLimiter:

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
  def fixedWindow(maxOperations: Int, windowDuration: Long, unit: TimeUnit): RateLimiter =
    FixedWindowRateLimiter(maxOperations, unit.toNanos(windowDuration))

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
  def slidingWindow(maxOperations: Int, windowDuration: Long, unit: TimeUnit): RateLimiter =
    SlidingWindowRateLimiter(maxOperations, unit.toNanos(windowDuration))

  /**
    * Create a token bucket rate limiter.
    *
    * Tokens are replenished at a fixed rate. Each operation consumes one token. If no tokens are
    * available, the operation blocks or is dropped. Allows bursts up to maxTokens capacity.
    *
    * @param maxTokens
    *   maximum tokens in the bucket (burst capacity)
    * @param refillRate
    *   number of tokens to add per refill interval
    * @param refillInterval
    *   interval between token refills
    * @param unit
    *   time unit for refillInterval
    */
  def tokenBucket(
      maxTokens: Int,
      refillRate: Int,
      refillInterval: Long,
      unit: TimeUnit
  ): RateLimiter = TokenBucketRateLimiter(maxTokens, refillRate, unit.toNanos(refillInterval))

  /**
    * Create a simple rate limiter that allows N operations per second.
    */
  def perSecond(operationsPerSecond: Int): RateLimiter = tokenBucket(
    operationsPerSecond,
    operationsPerSecond,
    1,
    TimeUnit.SECONDS
  )

  /**
    * Create a simple rate limiter that allows N operations per minute.
    */
  def perMinute(operationsPerMinute: Int): RateLimiter = tokenBucket(
    operationsPerMinute,
    operationsPerMinute,
    1,
    TimeUnit.MINUTES
  )

  // Internal implementations

  private class FixedWindowRateLimiter(maxOperations: Int, windowNanos: Long) extends RateLimiter:
    private val lock         = ReentrantLock()
    private var windowStart  = System.nanoTime()
    private var currentCount = 0L

    override def tryAcquire(): Boolean =
      lock.lock()
      try
        val now           = System.nanoTime()
        val currentWindow = windowStart

        // Check if we need to start a new window
        if now - currentWindow >= windowNanos then
          windowStart = now
          currentCount = 1
          true
        else if currentCount < maxOperations then
          currentCount += 1
          true
        else
          false
      finally
        lock.unlock()

    override def acquire(): Unit =
      while !tryAcquire() do
        val sleepMillis =
          lock.lock()
          try
            val now           = System.nanoTime()
            val currentWindow = windowStart
            if now - currentWindow >= windowNanos then
              0L // Window has been reset, try to acquire again immediately
            else
              val remainingNanos = windowNanos - (now - currentWindow)
              (remainingNanos / 1000000).max(1)
          finally
            lock.unlock()

        if sleepMillis > 0 then
          Thread.sleep(sleepMillis)

    override def runBlocking[A](operation: => A): A =
      acquire()
      operation

    override def runOrDrop[A](operation: => A): Option[A] =
      if tryAcquire() then
        Some(operation)
      else
        None

    override def availablePermits: Int =
      lock.lock()
      try
        val now           = System.nanoTime()
        val currentWindow = windowStart
        if now - currentWindow >= windowNanos then
          maxOperations
        else
          (maxOperations - currentCount.toInt).max(0)
      finally
        lock.unlock()

  end FixedWindowRateLimiter

  private class SlidingWindowRateLimiter(maxOperations: Int, windowNanos: Long) extends RateLimiter:
    private val lock       = ReentrantLock()
    private val timestamps = ArrayDeque[Long]()

    private def cleanupOldEntries(now: Long): Unit =
      val cutoff = now - windowNanos
      while timestamps.nonEmpty && timestamps.head < cutoff do
        timestamps.removeHead()

    override def tryAcquire(): Boolean =
      lock.lock()
      try
        val now = System.nanoTime()
        cleanupOldEntries(now)

        if timestamps.size < maxOperations then
          timestamps.addOne(now)
          true
        else
          false
      finally
        lock.unlock()

    override def acquire(): Unit =
      while !tryAcquire() do
        lock.lock()
        val sleepNanos =
          try
            val now = System.nanoTime()
            cleanupOldEntries(now)
            if timestamps.nonEmpty then
              val oldestTimestamp = timestamps.head
              val waitUntil       = oldestTimestamp + windowNanos
              (waitUntil - now).max(1000000) // At least 1ms
            else
              1000000 // 1ms default
          finally
            lock.unlock()
        Thread.sleep(sleepNanos / 1000000, (sleepNanos % 1000000).toInt)

    override def runBlocking[A](operation: => A): A =
      acquire()
      operation

    override def runOrDrop[A](operation: => A): Option[A] =
      if tryAcquire() then
        Some(operation)
      else
        None

    override def availablePermits: Int =
      lock.lock()
      try
        cleanupOldEntries(System.nanoTime())
        (maxOperations - timestamps.size).max(0)
      finally
        lock.unlock()

  end SlidingWindowRateLimiter

  private class TokenBucketRateLimiter(maxTokens: Int, refillRate: Int, refillIntervalNanos: Long)
      extends RateLimiter:
    private val lock           = ReentrantLock()
    private var tokens         = maxTokens.toLong
    private var lastRefillTime = System.nanoTime()

    private def refillTokens(): Unit =
      val now              = System.nanoTime()
      val lastRefill       = lastRefillTime
      val elapsedNanos     = now - lastRefill
      val intervalsElapsed = elapsedNanos / refillIntervalNanos
      if intervalsElapsed > 0 then
        val tokensToAdd = intervalsElapsed * refillRate
        tokens = (tokens + tokensToAdd).min(maxTokens.toLong)
        lastRefillTime = lastRefill + intervalsElapsed * refillIntervalNanos

    override def tryAcquire(): Boolean =
      lock.lock()
      try
        refillTokens()
        if tokens > 0 then
          tokens -= 1
          true
        else
          false
      finally
        lock.unlock()

    override def acquire(): Unit =
      while !tryAcquire() do
        lock.lock()
        val sleepNanos =
          try
            refillTokens()
            if tokens > 0 then
              0L
            else
              // Wait until next refill
              val now        = System.nanoTime()
              val lastRefill = lastRefillTime
              val nextRefill = lastRefill + refillIntervalNanos
              (nextRefill - now).max(1000000)
          finally
            lock.unlock()
        if sleepNanos > 0 then
          Thread.sleep(sleepNanos / 1000000, (sleepNanos % 1000000).toInt)

    override def runBlocking[A](operation: => A): A =
      acquire()
      operation

    override def runOrDrop[A](operation: => A): Option[A] =
      if tryAcquire() then
        Some(operation)
      else
        None

    override def availablePermits: Int =
      lock.lock()
      try
        refillTokens()
        tokens.toInt
      finally
        lock.unlock()

  end TokenBucketRateLimiter

end RateLimiter
