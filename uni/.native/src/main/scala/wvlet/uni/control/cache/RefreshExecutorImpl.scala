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

/**
  * Scala Native implementation - executes synchronously since native threading is limited.
  */
private[cache] object RefreshExecutorImpl:
  def create(): RefreshExecutor = new NativeRefreshExecutor()

private class NativeRefreshExecutor extends RefreshExecutor:
  override def submit(task: () => Unit): Boolean =
    // Execute synchronously on Native
    task()
    true
