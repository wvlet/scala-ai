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

import scala.scalajs.js
import scala.scalajs.js.timers
import java.util.concurrent.TimeUnit

/**
  * Scala.js-specific scheduler using the JavaScript event loop.
  *
  * JavaScript is single-threaded, so parallelism is always 1. Tasks are scheduled via the microtask
  * queue (Promise.resolve().then()) for immediate execution, or via setTimeout for delayed
  * execution.
  */
private[rx] object schedulerCompat:

  lazy val defaultScheduler: RxScheduler  = new JsScheduler()
  lazy val blockingScheduler: RxScheduler = defaultScheduler // Same on JS

  def createScheduler(parallelism: Int): RxScheduler =
    // Parallelism is ignored on JS - always single-threaded
    defaultScheduler

  private class JsScheduler extends RxScheduler:

    // Always 1 on JavaScript
    override val parallelism: Int = 1

    override def execute(task: Runnable): Unit =
      // Use microtask queue for immediate execution
      js.Promise.resolve(()).`then`(_ => task.run())

    override def schedule(delay: Long, unit: TimeUnit)(task: => Unit): Cancelable =
      val millis = unit.toMillis(delay)
      val handle = timers.setTimeout(millis.toDouble)(task)
      Cancelable { () =>
        timers.clearTimeout(handle)
      }

    override def scheduleAtFixedRate(initialDelay: Long, period: Long, unit: TimeUnit)(
        task: => Unit
    ): Cancelable =
      val initialMillis = unit.toMillis(initialDelay)
      val periodMillis  = unit.toMillis(period)

      var handle: timers.SetIntervalHandle = null
      var cancelled                        = false

      // Schedule initial delay
      val initialHandle =
        timers.setTimeout(initialMillis.toDouble) {
          if !cancelled then
            task
            // Start periodic execution
            handle = timers.setInterval(periodMillis.toDouble)(task)
        }

      Cancelable { () =>
        cancelled = true
        timers.clearTimeout(initialHandle)
        if handle != null then
          timers.clearInterval(handle)
      }

    override def shutdown(): Unit = () // No-op on JS - event loop is managed by the runtime

  end JsScheduler

end schedulerCompat
