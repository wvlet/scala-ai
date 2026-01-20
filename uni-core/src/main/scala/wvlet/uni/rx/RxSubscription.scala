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

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
  * A subscription that supports demand-based backpressure.
  *
  * This follows the Reactive Streams pattern where the subscriber controls the flow of data by
  * requesting elements. The publisher will not emit more elements than requested.
  *
  * Usage:
  * {{{
  *   val subscription = rx.subscribeWithBackpressure { event =>
  *     // process event
  *   }
  *   subscription.request(10)  // Request 10 elements
  *   // ... later
  *   subscription.request(5)   // Request 5 more elements
  *   subscription.cancel()     // Stop the subscription
  * }}}
  */
trait RxSubscription extends Cancelable:

  /**
    * Request n more elements from the upstream.
    *
    * This is additive - calling request(5) then request(3) means requesting 8 elements total.
    *
    * @param n
    *   the number of elements to request (must be positive)
    */
  def request(n: Long): Unit

  /**
    * Get the current unfulfilled demand.
    */
  def pendingDemand: Long

  /**
    * Check if the subscription is still active.
    */
  def isActive: Boolean

end RxSubscription

object RxSubscription:

  /**
    * A subscription with no backpressure (unbounded demand).
    */
  val unbounded: RxSubscription = new RxSubscription:
    private val cancelled = new AtomicReference[Boolean](false)

    override def request(n: Long): Unit = ()
    override def pendingDemand: Long    = Long.MaxValue
    override def isActive: Boolean      = !cancelled.get()
    override def cancel: Unit           = cancelled.set(true)

  /**
    * Create a new subscription that tracks demand.
    *
    * @param onRequest
    *   callback invoked when demand is added (with new total demand)
    * @param onCancel
    *   callback invoked when cancelled
    */
  def apply(onRequest: Long => Unit = _ => (), onCancel: () => Unit = () => ()): RxSubscription =
    new RxSubscriptionImpl(onRequest, onCancel)

  private class RxSubscriptionImpl(
      onRequest: Long => Unit,
      onCancel: () => Unit
  ) extends RxSubscription:
    private val demand    = new AtomicLong(0)
    private val cancelled = new AtomicReference[Boolean](false)

    override def request(n: Long): Unit =
      require(n > 0, s"Demand must be positive: ${n}")
      if isActive then
        val newDemand = demand.updateAndGet { current =>
          if current == Long.MaxValue then
            Long.MaxValue
          else
            val sum = current + n
            if sum < 0 then
              Long.MaxValue // Overflow protection
            else
              sum
        }
        onRequest(newDemand)

    override def pendingDemand: Long = demand.get()

    override def isActive: Boolean = !cancelled.get()

    override def cancel: Unit =
      if cancelled.compareAndSet(false, true) then
        onCancel()

    /**
      * Called by the publisher when an element is emitted. Returns true if there was demand.
      */
    private[rx] def consumeDemand(): Boolean =
      if !isActive then
        false
      else
        val current = demand.get()
        if current == Long.MaxValue then
          true
        else if current > 0 then
          demand.decrementAndGet()
          true
        else
          false

    /**
      * Set unbounded demand.
      */
    private[rx] def setUnbounded(): Unit = demand.set(Long.MaxValue)

  end RxSubscriptionImpl

end RxSubscription
