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
  * Platform-specific executor for background cache refresh operations.
  */
trait RefreshExecutor:
  /**
    * Submit a task for background execution.
    *
    * @return
    *   true if the task was submitted, false if rejected
    */
  def submit(task: () => Unit): Boolean

object RefreshExecutor:
  /**
    * Creates a platform-specific refresh executor.
    */
  def create(): RefreshExecutor = RefreshExecutorImpl.create()
