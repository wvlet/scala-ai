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
package wvlet.uni.control.cache

import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
  * JVM implementation using a daemon thread pool.
  */
private[cache] object RefreshExecutorImpl:
  def create(): RefreshExecutor = new JvmRefreshExecutor()

private class JvmRefreshExecutor extends RefreshExecutor:
  private lazy val executor = Executors.newSingleThreadExecutor { (r: Runnable) =>
    val t = new Thread(r, "cache-refresh")
    t.setDaemon(true)
    t
  }

  override def submit(task: () => Unit): Boolean =
    try
      executor.execute(() => task())
      true
    catch
      case _: RejectedExecutionException =>
        false
