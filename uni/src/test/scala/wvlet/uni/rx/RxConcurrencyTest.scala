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

import wvlet.uni.test.UniTest

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import scala.util.Failure
import scala.util.Success

/**
  * Tests for uni-rx concurrency primitives.
  */
object RxConcurrencyTest extends UniTest:

  // ==================== RxRef Tests ====================

  test("RxRef: get and set") {
    val ref = RxRef(42)
    ref.get.await shouldBe 42

    ref.set(100).await
    ref.get.await shouldBe 100
  }

  test("RxRef: getAndSet") {
    val ref = RxRef("hello")
    val old = ref.getAndSet("world").await
    old shouldBe "hello"
    ref.get.await shouldBe "world"
  }

  test("RxRef: update atomically") {
    val ref = RxRef(0)

    // Concurrent updates
    val latch   = new CountDownLatch(10)
    val threads = (1 to 10).map { _ =>
      new Thread(() =>
        ref.update(_ + 1).await
        latch.countDown()
      )
    }

    threads.foreach(_.start())
    latch.await(5, TimeUnit.SECONDS) shouldBe true
    ref.get.await shouldBe 10
  }

  test("RxRef: modify returns result") {
    val ref    = RxRef(List(1, 2, 3))
    val result =
      ref
        .modify { list =>
          val head = list.head
          (list.tail, head)
        }
        .await

    result shouldBe 1
    ref.get.await shouldBe List(2, 3)
  }

  test("RxRef: compareAndSet") {
    val ref = RxRef(42)

    ref.compareAndSet(42, 100).await shouldBe true
    ref.get.await shouldBe 100

    ref.compareAndSet(42, 200).await shouldBe false
    ref.get.await shouldBe 100
  }

  // ==================== RxDeferred Tests ====================

  test("RxDeferred: complete and get") {
    val deferred = RxDeferred[Int]()

    // Complete in a separate thread
    new Thread(() =>
      Thread.sleep(50)
      deferred.complete(42).await
    ).start()

    val result = deferred.get.await
    result shouldBe 42
  }

  test("RxDeferred: tryGet before and after completion") {
    val deferred = RxDeferred[String]()

    deferred.tryGet.await shouldBe None
    deferred.isCompleted.await shouldBe false

    deferred.complete("done").await shouldBe true
    deferred.tryGet.await shouldBe Some("done")
    deferred.isCompleted.await shouldBe true
  }

  test("RxDeferred: can only complete once") {
    val deferred = RxDeferred[Int]()

    deferred.complete(1).await shouldBe true
    deferred.complete(2).await shouldBe false
    deferred.get.await shouldBe 1
  }

  test("RxDeferred: multiple waiters") {
    val deferred = RxDeferred[Int]()
    val results  = new AtomicReference[List[Int]](Nil)
    val latch    = new CountDownLatch(3)

    // Start multiple waiters
    (1 to 3).foreach { _ =>
      new Thread(() =>
        val value = deferred.get.await
        results.updateAndGet(value :: _)
        latch.countDown()
      ).start()
    }

    Thread.sleep(50) // Let waiters register
    deferred.complete(42).await

    latch.await(5, TimeUnit.SECONDS) shouldBe true
    results.get().toSet shouldBe Set(42, 42, 42)
  }

  // ==================== RxSemaphore Tests ====================

  test("RxSemaphore: acquire and release") {
    val sem = RxSemaphore(2)

    sem.available.await shouldBe 2

    sem.acquire.await
    sem.available.await shouldBe 1

    sem.acquire.await
    sem.available.await shouldBe 0

    sem.release.await
    sem.available.await shouldBe 1
  }

  test("RxSemaphore: tryAcquire") {
    val sem = RxSemaphore(1)

    sem.tryAcquire.await shouldBe true
    sem.available.await shouldBe 0

    sem.tryAcquire.await shouldBe false
    sem.available.await shouldBe 0

    sem.release.await
    sem.tryAcquire.await shouldBe true
  }

  test("RxSemaphore: withPermit") {
    val sem    = RxSemaphore(1)
    val result = sem.withPermit(Rx.single(42)).await

    result shouldBe 42
    sem.available.await shouldBe 1 // Permit was released
  }

  test("RxSemaphore: limits concurrency") {
    val sem           = RxSemaphore(2)
    val maxConcurrent = new AtomicInteger(0)
    val current       = new AtomicInteger(0)
    val latch         = new CountDownLatch(5)

    (1 to 5).foreach { _ =>
      new Thread(() =>
        sem.acquire.await
        val c = current.incrementAndGet()
        maxConcurrent.updateAndGet(m => math.max(m, c))
        Thread.sleep(50)
        current.decrementAndGet()
        sem.release.await
        latch.countDown()
      ).start()
    }

    latch.await(5, TimeUnit.SECONDS) shouldBe true
    maxConcurrent.get() shouldBe 2
  }

  // ==================== RxScheduler Tests ====================

  test("RxScheduler: execute task") {
    val scheduler = RxScheduler.default
    val result    = new AtomicInteger(0)
    val latch     = new CountDownLatch(1)

    scheduler.execute(() =>
      result.set(42)
      latch.countDown()
    )

    latch.await(1, TimeUnit.SECONDS) shouldBe true
    result.get() shouldBe 42
  }

  test("RxScheduler: schedule delayed task") {
    val scheduler = RxScheduler.default
    val result    = new AtomicInteger(0)
    val latch     = new CountDownLatch(1)
    val start     = System.currentTimeMillis()

    scheduler.schedule(100, TimeUnit.MILLISECONDS) {
      result.set((System.currentTimeMillis() - start).toInt)
      latch.countDown()
    }

    latch.await(1, TimeUnit.SECONDS) shouldBe true
    result.get() should be >= 100
  }

  test("RxScheduler: cancel scheduled task") {
    val scheduler  = RxScheduler.default
    val executed   = new AtomicInteger(0)
    val cancelable =
      scheduler.schedule(100, TimeUnit.MILLISECONDS) {
        executed.incrementAndGet()
      }

    cancelable.cancel
    Thread.sleep(200)
    executed.get() shouldBe 0
  }

  // ==================== RxFiber Tests ====================

  test("RxFiber: start and join") {
    val result = Rx.single(42).start.flatMap(_.join).await
    result shouldBe 42
  }

  test("RxFiber: poll before and after completion") {
    val deferred = RxDeferred[Int]()
    val fiber    = deferred.get.start.await

    fiber.poll.await shouldBe None

    deferred.complete(42).await
    Thread.sleep(50) // Allow fiber to complete

    fiber.poll.await shouldMatch { case Some(Success(42)) =>
    }
  }

  test("RxFiber: cancel") {
    val started  = new CountDownLatch(1)
    val canceled = new CountDownLatch(1)

    val fiber =
      Rx.single {
          started.countDown()
          Thread.sleep(5000) // Long-running task
          42
        }
        .start
        .await

    started.await(1, TimeUnit.SECONDS)
    fiber.cancel.await
    fiber.isCancelled.await shouldBe true
  }

  // ==================== RxParallel Tests ====================

  test("RxParallel: parSequence") {
    val rxs    = (1 to 5).map(i => Rx.single(i * 2))
    val result = RxParallel.parSequence(rxs).await

    result shouldBe Seq(2, 4, 6, 8, 10)
  }

  test("RxParallel: parSequenceN with bounded parallelism") {
    val maxConcurrent = new AtomicInteger(0)
    val current       = new AtomicInteger(0)

    val rxs = (1 to 10).map { i =>
      Rx.single {
        val c = current.incrementAndGet()
        maxConcurrent.updateAndGet(m => math.max(m, c))
        Thread.sleep(20)
        current.decrementAndGet()
        i
      }
    }

    val result = RxParallel.parSequenceN(3)(rxs).await
    result shouldBe (1 to 10).toSeq
    maxConcurrent.get() should be <= 3
  }

  test("RxParallel: parTraverse") {
    val result = RxParallel.parTraverse(1 to 5)(i => Rx.single(i * 2)).await
    result shouldBe Seq(2, 4, 6, 8, 10)
  }

  test("RxParallel: parZip") {
    val a = Rx.single {
      Thread.sleep(50);
      1
    }
    val b = Rx.single {
      Thread.sleep(50);
      2
    }

    val start   = System.currentTimeMillis()
    val result  = RxParallel.parZip(a, b).await
    val elapsed = System.currentTimeMillis() - start

    result shouldBe (1, 2)
    // Should run in parallel, so < 100ms total
    elapsed should be < 100L
  }

  // ==================== RxResource Tests ====================

  test("RxResource: use with cleanup") {
    val closed = new AtomicInteger(0)

    val resource = RxResource.make(Rx.single("resource"))(_ => Rx.single(closed.incrementAndGet()))

    val result = resource.use(r => Rx.single(r.length)).await
    result shouldBe 8
    closed.get() shouldBe 1
  }

  test("RxResource: cleanup on error") {
    val closed = new AtomicInteger(0)

    val resource = RxResource.make(Rx.single("resource"))(_ => Rx.single(closed.incrementAndGet()))

    val result = scala
      .util
      .Try {
        resource.use(_ => Rx.exception(new RuntimeException("boom"))).await
      }

    result.isFailure shouldBe true
    closed.get() shouldBe 1 // Cleanup still ran
  }

  test("RxResource: fromAutoCloseable") {
    val closed = new AtomicInteger(0)

    class TestCloseable extends AutoCloseable:
      def getValue: Int          = 42
      override def close(): Unit = closed.incrementAndGet()

    val resource = RxResource.fromAutoCloseable(Rx.single(new TestCloseable))

    val result = resource.use(tc => Rx.single(tc.getValue)).await
    result shouldBe 42
    closed.get() shouldBe 1
  }

  // ==================== RxBoundedQueue Tests ====================

  test("RxBoundedQueue: offer and take") {
    val queue = RxBoundedQueue[Int](10)

    queue.offer(1).await
    queue.offer(2).await
    queue.offer(3).await

    queue.size.await shouldBe 3

    queue.take.await shouldBe 1
    queue.take.await shouldBe 2
    queue.take.await shouldBe 3

    queue.isEmpty.await shouldBe true
  }

  test("RxBoundedQueue: tryOffer and tryTake") {
    val queue = RxBoundedQueue[Int](2)

    queue.tryOffer(1).await shouldBe true
    queue.tryOffer(2).await shouldBe true
    queue.tryOffer(3).await shouldBe false // Queue full

    queue.tryTake.await shouldBe Some(1)
    queue.tryTake.await shouldBe Some(2)
    queue.tryTake.await shouldBe None // Queue empty
  }

  test("RxBoundedQueue: backpressure") {
    val queue    = RxBoundedQueue[Int](2)
    val produced = new AtomicInteger(0)
    val consumed = new AtomicInteger(0)
    val latch    = new CountDownLatch(5)

    // Producer thread
    new Thread(() =>
      (1 to 5).foreach { i =>
        queue.offer(i).await
        produced.incrementAndGet()
      }
    ).start()

    // Consumer thread with delay
    new Thread(() =>
      (1 to 5).foreach { _ =>
        Thread.sleep(20) // Slow consumer
        queue.take.await
        consumed.incrementAndGet()
        latch.countDown()
      }
    ).start()

    latch.await(5, TimeUnit.SECONDS) shouldBe true
    produced.get() shouldBe 5
    consumed.get() shouldBe 5
  }

end RxConcurrencyTest
