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

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
  * Scala Native-specific scheduler using standard thread pools.
  *
  * Scala Native supports multithreading, so we use a fixed-size thread pool.
  */
private[rx] object schedulerCompat:

  lazy val defaultScheduler: RxScheduler = createScheduler(Runtime.getRuntime.availableProcessors())

  lazy val blockingScheduler: RxScheduler = new NativeBlockingScheduler()

  def createScheduler(parallelism: Int): RxScheduler = new NativeScheduler(parallelism)

  private class NativeScheduler(val parallelism: Int) extends RxScheduler:
    private val pool: ScheduledExecutorService = Executors.newScheduledThreadPool(parallelism)

    override def execute(task: Runnable): Unit = pool.execute(task)

    override def schedule(delay: Long, unit: TimeUnit)(task: => Unit): Cancelable =
      val cancelled = new AtomicBoolean(false)
      val future    = pool.schedule(
        new Runnable:
          override def run(): Unit =
            if !cancelled.get() then
              task
        ,
        delay,
        unit
      )
      Cancelable { () =>
        cancelled.set(true)
        future.cancel(false)
      }

    override def scheduleAtFixedRate(initialDelay: Long, period: Long, unit: TimeUnit)(
        task: => Unit
    ): Cancelable =
      val cancelled = new AtomicBoolean(false)
      val future    = pool.scheduleAtFixedRate(
        new Runnable:
          override def run(): Unit =
            if !cancelled.get() then
              task
        ,
        initialDelay,
        period,
        unit
      )
      Cancelable { () =>
        cancelled.set(true)
        future.cancel(false)
      }

    override def shutdown(): Unit = pool.shutdown()

  end NativeScheduler

  private class NativeBlockingScheduler extends RxScheduler:
    private val pool: ScheduledExecutorService = Executors.newCachedThreadPool()

    override val parallelism: Int = Int.MaxValue

    override def execute(task: Runnable): Unit = pool.execute(task)

    override def schedule(delay: Long, unit: TimeUnit)(task: => Unit): Cancelable =
      val cancelled       = new AtomicBoolean(false)
      val delayedExecutor = Executors.newSingleThreadScheduledExecutor()
      val future          = delayedExecutor.schedule(
        new Runnable:
          override def run(): Unit =
            if !cancelled.get() then
              pool.execute(() => task)
        ,
        delay,
        unit
      )
      Cancelable { () =>
        cancelled.set(true)
        future.cancel(false)
        delayedExecutor.shutdown()
      }

    override def scheduleAtFixedRate(initialDelay: Long, period: Long, unit: TimeUnit)(
        task: => Unit
    ): Cancelable =
      val cancelled       = new AtomicBoolean(false)
      val delayedExecutor = Executors.newSingleThreadScheduledExecutor()
      val future          = delayedExecutor.scheduleAtFixedRate(
        new Runnable:
          override def run(): Unit =
            if !cancelled.get() then
              pool.execute(() => task)
        ,
        initialDelay,
        period,
        unit
      )
      Cancelable { () =>
        cancelled.set(true)
        future.cancel(false)
        delayedExecutor.shutdown()
      }

    override def shutdown(): Unit = pool.shutdown()

  end NativeBlockingScheduler

end schedulerCompat
