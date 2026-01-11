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
package wvlet.uni.core.util

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

object ThreadUtil:

  /**
    * Sleep the current thread for the specified duration. Note that this method is no-op in
    * Scala.js, which has no way to hold the current thread.
    * @param millis
    *   milliseconds to sleep the thread.
    */
  def sleep(millis: Int): Unit = compat.sleep(millis)

  /**
    * Create a thread factory for daemon threads, which do not block JVM shutdown
    *
    * @param name
    *   the name of the new thread group. New threds will be named (name)-1, (name)-2, etc.
    */
  def newDaemonThreadFactory(name: String): ThreadFactory =
    new ThreadFactory:
      private val group: ThreadGroup =
        new ThreadGroup(Thread.currentThread().getThreadGroup(), name)
      private val threadNumber                    = new AtomicInteger(1)
      override def newThread(r: Runnable): Thread =
        val threadName = s"${name}-${threadNumber.getAndIncrement()}"
        val thread     = new Thread(group, r, threadName)
        thread.setName(threadName)
        thread.setDaemon(true)
        thread
