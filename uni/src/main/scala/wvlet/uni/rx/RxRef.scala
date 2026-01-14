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

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec

/**
  * A lock-free concurrent state container, inspired by cats-effect Ref.
  *
  * RxRef provides atomic read-modify-write operations on a mutable reference, safe for concurrent
  * access from multiple threads. All operations are non-blocking and use compare-and-swap (CAS) for
  * atomicity.
  *
  * Unlike RxVar, RxRef does not emit change events to subscribers. It is purely a concurrent state
  * container.
  *
  * @tparam A
  *   the type of the value stored in the reference
  */
trait RxRef[A]:

  /**
    * Get the current value.
    */
  def get: Rx[A]

  /**
    * Set a new value.
    */
  def set(a: A): Rx[Unit]

  /**
    * Get the current value and set a new value atomically.
    */
  def getAndSet(a: A): Rx[A]

  /**
    * Atomically update the value using the given function. The function may be called multiple
    * times if there is contention.
    */
  def update(f: A => A): Rx[Unit]

  /**
    * Atomically update the value and return the old value.
    */
  def getAndUpdate(f: A => A): Rx[A]

  /**
    * Atomically update the value and return the new value.
    */
  def updateAndGet(f: A => A): Rx[A]

  /**
    * Atomically modify the value and return a result. The function takes the current value and
    * returns a tuple of (new value, result).
    */
  def modify[B](f: A => (A, B)): Rx[B]

  /**
    * Compare and set: atomically set the value to `newValue` if the current value is `expected`.
    * Returns true if the swap was successful.
    */
  def compareAndSet(expected: A, newValue: A): Rx[Boolean]

  /**
    * Get the current value without wrapping in Rx. Use with caution in concurrent contexts.
    */
  def unsafeGet: A

end RxRef

object RxRef:

  /**
    * Create a new RxRef with the given initial value.
    */
  def apply[A](initial: A): RxRef[A] = new RxRefImpl(initial)

  /**
    * Create a new RxRef wrapped in Rx.
    */
  def of[A](initial: A): Rx[RxRef[A]] = Rx.single(apply(initial))

  private class RxRefImpl[A](initial: A) extends RxRef[A]:
    private val ref = new AtomicReference[A](initial)

    override def get: Rx[A] = Rx.single(ref.get())

    override def set(a: A): Rx[Unit] = Rx.single(ref.set(a))

    override def getAndSet(a: A): Rx[A] = Rx.single(ref.getAndSet(a))

    override def update(f: A => A): Rx[Unit] = Rx.single {
      updateLoop(f)
      ()
    }

    override def getAndUpdate(f: A => A): Rx[A] = Rx.single {
      updateLoop(f)
    }

    override def updateAndGet(f: A => A): Rx[A] = Rx.single {
      updateAndGetLoop(f)
    }

    override def modify[B](f: A => (A, B)): Rx[B] = Rx.single {
      modifyLoop(f)
    }

    override def compareAndSet(expected: A, newValue: A): Rx[Boolean] = Rx.single {
      ref.compareAndSet(expected, newValue)
    }

    override def unsafeGet: A = ref.get()

    @tailrec
    private def updateLoop(f: A => A): A =
      val current = ref.get()
      val next    = f(current)
      if ref.compareAndSet(current, next) then
        current
      else
        updateLoop(f)

    @tailrec
    private def updateAndGetLoop(f: A => A): A =
      val current = ref.get()
      val next    = f(current)
      if ref.compareAndSet(current, next) then
        next
      else
        updateAndGetLoop(f)

    @tailrec
    private def modifyLoop[B](f: A => (A, B)): B =
      val current        = ref.get()
      val (next, result) = f(current)
      if ref.compareAndSet(current, next) then
        result
      else
        modifyLoop(f)

  end RxRefImpl

end RxRef
