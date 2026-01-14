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
package wvlet.uni.rx

import org.scalacheck.Gen
import wvlet.uni.test.PropertyCheck
import wvlet.uni.test.UniTest

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import scala.util.Random

/**
  * Property-based tests verifying concurrency invariants for uni-rx primitives.
  *
  * These tests use ScalaCheck to generate random inputs and verify that concurrency properties hold
  * under various conditions.
  */
class RxConcurrencyPropertyTest extends UniTest with PropertyCheck:

  private val threadPool = Executors.newFixedThreadPool(8)

  override protected def afterAll(): Unit =
    threadPool.shutdown()
    threadPool.awaitTermination(5, TimeUnit.SECONDS)

  // ==================== RxRef Property Tests ====================

  test("RxRef: concurrent updates preserve count invariant") {
    // Property: If N threads each increment a counter M times,
    // the final value should be N * M
    forAll(Gen.choose(2, 8), Gen.choose(10, 100)) { (numThreads: Int, incrementsPerThread: Int) =>
      val ref          = RxRef(0)
      val latch        = CountDownLatch(numThreads)
      val startBarrier = CountDownLatch(1)

      // Launch threads that will increment concurrently
      (1 to numThreads).foreach { _ =>
        threadPool.execute(() =>
          startBarrier.await() // Wait for all threads to be ready
          (1 to incrementsPerThread).foreach { _ =>
            ref.update(_ + 1).await
          }
          latch.countDown()
        )
      }

      // Start all threads simultaneously
      startBarrier.countDown()
      latch.await(10, TimeUnit.SECONDS) shouldBe true

      // Verify the invariant
      ref.unsafeGet shouldBe (numThreads * incrementsPerThread)
    }
  }

  test("RxRef: modify returns correct accumulated result") {
    // Property: modify should return the old value and update atomically
    forAll(Gen.choose(2, 6), Gen.choose(5, 20)) { (numThreads: Int, opsPerThread: Int) =>
      val ref          = RxRef(0)
      val sum          = AtomicInteger(0)
      val latch        = CountDownLatch(numThreads)
      val startBarrier = CountDownLatch(1)

      // Each thread will atomically increment and collect old values
      (1 to numThreads).foreach { _ =>
        threadPool.execute(() =>
          startBarrier.await()
          (1 to opsPerThread).foreach { _ =>
            val oldValue = ref.modify(v => (v + 1, v)).await
            sum.addAndGet(oldValue)
          }
          latch.countDown()
        )
      }

      startBarrier.countDown()
      latch.await(10, TimeUnit.SECONDS) shouldBe true

      // The sum of all old values should be 0 + 1 + 2 + ... + (N*M - 1)
      val totalOps    = numThreads * opsPerThread
      val expectedSum = (0 until totalOps).sum
      sum.get() shouldBe expectedSum
    }
  }

  test("RxRef: compareAndSet succeeds exactly once for same expected value") {
    // Property: When multiple threads try CAS with same expected value,
    // exactly one should succeed
    forAll(Gen.choose(2, 10)) { (numThreads: Int) =>
      val ref           = RxRef(0)
      val successCount  = AtomicInteger(0)
      val latch         = CountDownLatch(numThreads)
      val startBarrier  = CountDownLatch(1)

      (1 to numThreads).foreach { i =>
        threadPool.execute(() =>
          startBarrier.await()
          val success = ref.compareAndSet(0, i).await
          if success then successCount.incrementAndGet()
          latch.countDown()
        )
      }

      startBarrier.countDown()
      latch.await(5, TimeUnit.SECONDS) shouldBe true

      // Exactly one CAS should succeed
      successCount.get() shouldBe 1
      // Final value should be non-zero (one of the thread IDs)
      (ref.unsafeGet >= 1 && ref.unsafeGet <= numThreads) shouldBe true
    }
  }

  // ==================== RxDeferred Property Tests ====================

  test("RxDeferred: only first completion succeeds") {
    // Property: When multiple threads try to complete, exactly one succeeds
    forAll(Gen.choose(2, 10)) { (numThreads: Int) =>
      val deferred      = RxDeferred[Int]()
      val successCount  = AtomicInteger(0)
      val latch         = CountDownLatch(numThreads)
      val startBarrier  = CountDownLatch(1)

      (1 to numThreads).foreach { i =>
        threadPool.execute(() =>
          startBarrier.await()
          val success = deferred.complete(i).await
          if success then successCount.incrementAndGet()
          latch.countDown()
        )
      }

      startBarrier.countDown()
      latch.await(5, TimeUnit.SECONDS) shouldBe true

      // Exactly one completion should succeed
      successCount.get() shouldBe 1
      deferred.isCompleted.await shouldBe true
    }
  }

  test("RxDeferred: all waiters receive the same value") {
    // Property: All threads waiting on get should receive the same value
    forAll(Gen.choose(2, 8), Gen.choose(1, 1000)) { (numWaiters: Int, completionValue: Int) =>
      val deferred       = RxDeferred[Int]()
      val receivedValues = java.util.concurrent.ConcurrentLinkedQueue[Int]()
      val latch          = CountDownLatch(numWaiters)
      val readyBarrier   = CountDownLatch(numWaiters)

      // Start waiter threads
      (1 to numWaiters).foreach { _ =>
        threadPool.execute(() =>
          readyBarrier.countDown()
          val value = deferred.get.await
          receivedValues.add(value)
          latch.countDown()
        )
      }

      // Wait for all waiters to be ready, then complete
      readyBarrier.await(5, TimeUnit.SECONDS)
      Thread.sleep(10) // Small delay to ensure waiters are blocked
      deferred.complete(completionValue).await

      latch.await(5, TimeUnit.SECONDS) shouldBe true

      // All waiters should receive the same value
      receivedValues.size() shouldBe numWaiters
      receivedValues.forEach(v => v shouldBe completionValue)
    }
  }

  // ==================== RxSemaphore Property Tests ====================

  test("RxSemaphore: permits are conserved") {
    // Property: Total permits (available + acquired) should always equal initial permits
    forAll(Gen.choose(1, 5), Gen.choose(2, 6), Gen.choose(5, 20)) {
      (initialPermits: Int, numThreads: Int, opsPerThread: Int) =>
        val sem          = RxSemaphore(initialPermits)
        val latch        = CountDownLatch(numThreads)
        val startBarrier = CountDownLatch(1)

        (1 to numThreads).foreach { _ =>
          threadPool.execute(() =>
            startBarrier.await()
            (1 to opsPerThread).foreach { _ =>
              if sem.tryAcquire.await then
                // Simulate some work
                Thread.sleep(Random.nextInt(2))
                sem.release.await
            }
            latch.countDown()
          )
        }

        startBarrier.countDown()
        latch.await(10, TimeUnit.SECONDS) shouldBe true

        // After all operations, all permits should be returned
        sem.available.await shouldBe initialPermits
    }
  }

  test("RxSemaphore: withPermit guarantees release") {
    // Property: withPermit should always release permit, even if body fails
    forAll(Gen.choose(1, 3), Gen.choose(2, 5)) { (permits: Int, numThreads: Int) =>
      val sem          = RxSemaphore(permits)
      val latch        = CountDownLatch(numThreads)
      val startBarrier = CountDownLatch(1)

      (1 to numThreads).foreach { i =>
        threadPool.execute(() =>
          startBarrier.await()
          try
            sem
              .withPermit {
                Rx.single {
                  Thread.sleep(Random.nextInt(5))
                  if i % 2 == 0 then throw RuntimeException("Simulated failure")
                  42
                }
              }
              .await
          catch case _: RuntimeException => () // Expected for even threads
          latch.countDown()
        )
      }

      startBarrier.countDown()
      latch.await(10, TimeUnit.SECONDS) shouldBe true

      // All permits should be returned despite failures
      sem.available.await shouldBe permits
    }
  }

  test("RxSemaphore: concurrent limit is respected") {
    // Property: Never more than N concurrent executions with N permits
    forAll(Gen.choose(1, 4), Gen.choose(4, 10)) { (permits: Int, numTasks: Int) =>
      val sem              = RxSemaphore(permits)
      val concurrentCount  = AtomicInteger(0)
      val maxConcurrent    = AtomicInteger(0)
      val latch            = CountDownLatch(numTasks)
      val startBarrier     = CountDownLatch(1)

      (1 to numTasks).foreach { _ =>
        threadPool.execute(() =>
          startBarrier.await()
          sem.acquire.await
          try
            val current = concurrentCount.incrementAndGet()
            // Track max concurrent
            maxConcurrent.updateAndGet(max => math.max(max, current))
            Thread.sleep(Random.nextInt(10) + 1)
          finally
            concurrentCount.decrementAndGet()
            sem.release.await
            latch.countDown()
        )
      }

      startBarrier.countDown()
      latch.await(10, TimeUnit.SECONDS) shouldBe true

      // Max concurrent should never exceed permits
      (maxConcurrent.get() <= permits) shouldBe true
      sem.available.await shouldBe permits
    }
  }

  // ==================== RxParallel Property Tests ====================

  test("parSequence: preserves result order") {
    // Property: Results should be in the same order as inputs regardless of completion order
    forAll(Gen.choose(2, 10)) { (n: Int) =>
      val inputs = (1 to n).toSeq
      val rxs = inputs.map { i =>
        Rx.single {
          // Random delay to vary completion order
          Thread.sleep(Random.nextInt(20))
          i * 10
        }
      }

      val results = RxParallel.parSequence(rxs).await

      results shouldBe inputs.map(_ * 10)
    }
  }

  test("parSequence: handles empty sequence") {
    val results = RxParallel.parSequence(Seq.empty[Rx[Int]]).await
    results shouldBe Seq.empty
  }

  test("parTraverse: applies function and preserves order") {
    // Property: parTraverse(as)(f) should equal parSequence(as.map(f)) in result
    forAll(Gen.listOfN(5, Gen.choose(1, 100))) { (inputs: List[Int]) =>
      val results = RxParallel.parTraverse(inputs)(i =>
        Rx.single {
          Thread.sleep(Random.nextInt(5))
          i * 2
        }
      ).await

      results shouldBe inputs.map(_ * 2)
    }
  }

  test("parZip: combines results correctly") {
    // Property: parZip should return tuple of both results
    forAll(Gen.choose(1, 1000), Gen.alphaStr) { (intVal: Int, strVal: String) =>
      val (a, b) = RxParallel
        .parZip(
          Rx.single { Thread.sleep(Random.nextInt(10)); intVal },
          Rx.single { Thread.sleep(Random.nextInt(10)); strVal }
        )
        .await

      a shouldBe intVal
      b shouldBe strVal
    }
  }

  test("race: returns first completed result") {
    // Property: race should return a result (we can't predict which one)
    forAll(Gen.choose(1, 100), Gen.choose(1, 100)) { (v1: Int, v2: Int) =>
      val result = RxParallel
        .race(
          Rx.single { Thread.sleep(Random.nextInt(10)); v1 },
          Rx.single { Thread.sleep(Random.nextInt(10)); v2 }
        )
        .await

      // Result should be one of the two values
      (result == v1 || result == v2) shouldBe true
    }
  }

  // ==================== RxBoundedQueue Property Tests ====================

  test("RxBoundedQueue: FIFO order is preserved") {
    // Property: Elements should come out in the same order they went in
    forAll(Gen.choose(5, 20)) { (n: Int) =>
      val queue  = RxBoundedQueue[Int](n)
      val inputs = (1 to n).toSeq

      // Offer all elements
      inputs.foreach(i => queue.tryOffer(i).await shouldBe true)

      // Take all elements
      val outputs = (1 to n).map(_ => queue.tryTake.await.get)

      outputs shouldBe inputs
    }
  }

  test("RxBoundedQueue: capacity is respected") {
    // Property: Queue should reject offers when full
    forAll(Gen.choose(1, 10)) { (capacity: Int) =>
      val queue = RxBoundedQueue[Int](capacity)

      // Fill the queue
      (1 to capacity).foreach { i =>
        queue.tryOffer(i).await shouldBe true
      }

      // Queue should be full
      queue.isFull.await shouldBe true
      queue.tryOffer(999).await shouldBe false

      // After taking one, we can offer again
      queue.tryTake.await shouldBe Some(1)
      queue.tryOffer(999).await shouldBe true
    }
  }

  test("RxBoundedQueue: size tracks correctly under concurrent access") {
    forAll(Gen.choose(5, 15), Gen.choose(2, 4)) { (capacity: Int, numProducers: Int) =>
      val queue        = RxBoundedQueue[Int](capacity)
      val latch        = CountDownLatch(numProducers * 2) // producers + consumers
      val startBarrier = CountDownLatch(1)
      val produced     = AtomicInteger(0)
      val consumed     = AtomicInteger(0)
      val itemsPerThread = capacity

      // Start producers
      (1 to numProducers).foreach { p =>
        threadPool.execute(() =>
          startBarrier.await()
          (1 to itemsPerThread).foreach { i =>
            while !queue.tryOffer(p * 100 + i).await do Thread.sleep(1)
            produced.incrementAndGet()
          }
          latch.countDown()
        )
      }

      // Start consumers
      (1 to numProducers).foreach { _ =>
        threadPool.execute(() =>
          startBarrier.await()
          (1 to itemsPerThread).foreach { _ =>
            while queue.tryTake.await.isEmpty do Thread.sleep(1)
            consumed.incrementAndGet()
          }
          latch.countDown()
        )
      }

      startBarrier.countDown()
      latch.await(10, TimeUnit.SECONDS) shouldBe true

      // All produced items should be consumed
      produced.get() shouldBe consumed.get()
      queue.isEmpty.await shouldBe true
    }
  }

end RxConcurrencyPropertyTest
