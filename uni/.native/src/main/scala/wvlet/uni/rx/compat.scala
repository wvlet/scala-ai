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

import wvlet.uni.util.ThreadUtil
import scala.util.Try
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.Promise
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import scala.scalanative.posix.timer.timer_create
import scala.scalanative.posix.time.*
import scala.scalanative.posix.signal.*
import scala.scalanative.posix.sys.types.timer_t
import scala.scalanative.unsafe.*

/**
  */
object compat:

  private lazy val defaultExecutor = ExecutionContext.fromExecutorService(
    Executors.newCachedThreadPool(ThreadUtil.newDaemonThreadFactory("airframe-rx"))
  )

  def defaultExecutionContext: scala.concurrent.ExecutionContext = defaultExecutor

  def newTimer: Timer = ???
//    Zone {
//      val timerId: Ptr[timer_t] = alloc[timer_t]()
//      val se: sigevent          = stackalloc[sigevent]()
//      se._1 = SIGEV_THREAD
////        sigev_notify = SIGEV_THREAD,
////        sigev_signo = SIGALRM,
////        sigev_value = sigval(0),
////        sigev_notify_thread_id = pthread_self()
////      )
//// timer_create is not yet implemented in Scala Native
//      // timer_create(CLOCK_REALTIME, se, timerId)

  private lazy val scheduledThreadManager = Executors.newScheduledThreadPool(
    1,
    ThreadUtil.newDaemonThreadFactory("rx-timer")
  )

  def scheduleOnce[U](delayMills: Long)(body: => U): Cancelable =
    val schedule = scheduledThreadManager.schedule(
      new Runnable:
        override def run(): Unit = body
      ,
      delayMills,
      TimeUnit.MILLISECONDS
    )
    Cancelable { () =>
      schedule.cancel(false)
    }

  def toSeq[A](rx: Rx[A]): Seq[A] =
    val ready = new AtomicBoolean(true)
    val s     = Seq.newBuilder[A]
    var c     = Cancelable.empty
    c =
      RxRunner.run(rx) {
        case OnNext(v) =>
          s += v.asInstanceOf[A]
        case OnError(e) =>
          c.cancel
          throw e
        case OnCompletion =>
          c.cancel
          ready.set(true)
      }
    while !ready.get() do {}
    s.result()

  private[rx] def await[A](rx: RxOps[A]): A =
    val p = Promise[A]()
    val c =
      RxRunner.runOnce(rx) {
        case OnNext(v) =>
          p.success(v.asInstanceOf[A])
        case OnError(e) =>
          p.failure(e)
        case OnCompletion =>
          p.failure(new IllegalStateException(s"OnCompletion should not be issued in: ${rx}"))
      }
    try Await.result(p.future, Duration.Inf)
    finally c.cancel

end compat
