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

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable.ArrayBuffer

/**
  * Tests for backpressure support in uni-rx.
  */
class RxBackpressureTest extends UniTest:

  private def collectEvents[A](rx: Rx[A]): Seq[RxEvent] =
    val events = Seq.newBuilder[RxEvent]
    RxRunner.run(rx)(events += _)
    events.result()

  private def collectValues[A](rx: Rx[A]): Seq[A] =
    collectEvents(rx).collect { case OnNext(v) =>
      v.asInstanceOf[A]
    }

  // ==================== RxResult Tests ====================

  test("RxResult: Continue has unbounded demand") {
    RxResult.Continue.toContinue shouldBe true
    RxResult.Continue.demand shouldBe Long.MaxValue
    RxResult.Continue.isUnbounded shouldBe true
    RxResult.Continue.isPaused shouldBe false
  }

  test("RxResult: Stop has zero demand") {
    RxResult.Stop.toContinue shouldBe false
    RxResult.Stop.demand shouldBe 0
    RxResult.Stop.isUnbounded shouldBe false
    RxResult.Stop.isPaused shouldBe false
  }

  test("RxResult: Paused has zero demand but continues") {
    RxResult.Paused.toContinue shouldBe true
    RxResult.Paused.demand shouldBe 0
    RxResult.Paused.isUnbounded shouldBe false
    RxResult.Paused.isPaused shouldBe true
  }

  test("RxResult: request creates demand") {
    val r1 = RxResult.request(10)
    r1.toContinue shouldBe true
    r1.demand shouldBe 10
    r1.isUnbounded shouldBe false
    r1.isPaused shouldBe false
  }

  test("RxResult: request(MaxValue) returns Continue") {
    val r = RxResult.request(Long.MaxValue)
    r shouldBe RxResult.Continue
  }

  test("RxResult: request(0) returns Paused") {
    val r = RxResult.request(0)
    r shouldBe RxResult.Paused
  }

  test("RxResult: && combines demands") {
    (RxResult.Continue && RxResult.Continue) shouldBe RxResult.Continue
    (RxResult.Continue && RxResult.Stop) shouldBe RxResult.Stop
    (RxResult.Stop && RxResult.Continue) shouldBe RxResult.Stop
    (RxResult.Stop && RxResult.Stop) shouldBe RxResult.Stop

    val r10 = RxResult.request(10)
    val r5  = RxResult.request(5)
    (r10 && r5).demand shouldBe 5
    (r5 && r10).demand shouldBe 5
    (RxResult.Continue && r5).demand shouldBe 5
    (r10 && RxResult.Continue).demand shouldBe 10
  }

  test("RxResult: decrementDemand") {
    RxResult.Continue.decrementDemand shouldBe RxResult.Continue
    RxResult.Stop.decrementDemand shouldBe RxResult.Stop

    val r10 = RxResult.request(10)
    r10.decrementDemand.demand shouldBe 9

    val r1 = RxResult.request(1)
    r1.decrementDemand shouldBe RxResult.Paused
  }

  test("RxResult: addDemand") {
    RxResult.Stop.addDemand(10) shouldBe RxResult.Stop
    RxResult.Continue.addDemand(10) shouldBe RxResult.Continue

    val r5 = RxResult.request(5)
    r5.addDemand(3).demand shouldBe 8
    r5.addDemand(Long.MaxValue) shouldBe RxResult.Continue
  }

  // ==================== RxSubscription Tests ====================

  test("RxSubscription: unbounded has MaxValue demand") {
    val sub = RxSubscription.unbounded
    sub.pendingDemand shouldBe Long.MaxValue
    sub.isActive shouldBe true

    sub.request(10)
    sub.pendingDemand shouldBe Long.MaxValue

    sub.cancel
    sub.isActive shouldBe false
  }

  test("RxSubscription: tracks demand") {
    val requests = ArrayBuffer.empty[Long]
    val sub      = RxSubscription(n => requests += n)

    sub.pendingDemand shouldBe 0
    sub.isActive shouldBe true

    sub.request(5)
    sub.pendingDemand shouldBe 5
    requests.last shouldBe 5

    sub.request(3)
    sub.pendingDemand shouldBe 8
    requests.last shouldBe 8
  }

  test("RxSubscription: cancel invokes callback") {
    var cancelled = false
    val sub       = RxSubscription(onCancel = () => cancelled = true)

    sub.isActive shouldBe true
    cancelled shouldBe false

    sub.cancel
    sub.isActive shouldBe false
    cancelled shouldBe true
  }

  // ==================== Buffer Operator Tests ====================

  test("buffer: buffers elements") {
    val values = collectValues(Rx.sequence(1, 2, 3, 4, 5).buffer(10))
    values shouldBe Seq(1, 2, 3, 4, 5)
  }

  test("buffer: requires positive capacity") {
    intercept[IllegalArgumentException] {
      Rx.sequence(1, 2, 3).buffer(0)
    }
    intercept[IllegalArgumentException] {
      Rx.sequence(1, 2, 3).buffer(-1)
    }
  }

  // ==================== BackpressureDrop Tests ====================

  test("onBackpressureDrop: passes elements when not busy") {
    val values = collectValues(Rx.sequence(1, 2, 3, 4, 5).onBackpressureDrop)
    values shouldBe Seq(1, 2, 3, 4, 5)
  }

  test("onBackpressureDrop: invokes callback on drop") {
    val dropped = ArrayBuffer.empty[Int]
    val rx      = Rx.sequence(1, 2, 3).onBackpressureDrop(dropped += _)
    collectValues(rx)
    // In synchronous mode, nothing should be dropped
  }

  // ==================== BackpressureBuffer Tests ====================

  test("onBackpressureBuffer: buffers elements") {
    val values = collectValues(Rx.sequence(1, 2, 3, 4, 5).onBackpressureBuffer(10))
    values shouldBe Seq(1, 2, 3, 4, 5)
  }

  test("onBackpressureBuffer: DropOldest strategy") {
    val rx     = Rx.sequence(1, 2, 3, 4, 5).onBackpressureBuffer(
      3,
      Rx.BackpressureOverflowStrategy.DropOldest
    )
    val values = collectValues(rx)
    // Elements are passed through in synchronous mode
    values shouldBe Seq(1, 2, 3, 4, 5)
  }

  test("onBackpressureBuffer: requires positive capacity") {
    intercept[IllegalArgumentException] {
      Rx.sequence(1, 2, 3).onBackpressureBuffer(0)
    }
  }

  // ==================== BackpressureLatest Tests ====================

  test("onBackpressureLatest: passes elements") {
    val values = collectValues(Rx.sequence(1, 2, 3, 4, 5).onBackpressureLatest)
    values shouldBe Seq(1, 2, 3, 4, 5)
  }

  // ==================== BackpressureOverflowStrategy Tests ====================

  test("BackpressureOverflowStrategy enum values") {
    Rx.BackpressureOverflowStrategy.DropOldest shouldMatch { case _: Rx.BackpressureOverflowStrategy =>
    }
    Rx.BackpressureOverflowStrategy.DropNewest shouldMatch { case _: Rx.BackpressureOverflowStrategy =>
    }
    Rx.BackpressureOverflowStrategy.Error shouldMatch { case _: Rx.BackpressureOverflowStrategy =>
    }
  }

  test("BackpressureOverflowException message") {
    val ex = Rx.BackpressureOverflowException(100)
    ex.getMessage shouldContain "100"
  }

end RxBackpressureTest
