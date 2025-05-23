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
package wvlet.ai.core.rx

/**
  * Rx implementation where the data is provided from an external process.
  */
trait RxSource[A] extends Rx[A]:
  def put(e: A): Unit = add(OnNext(e))
  def add(ev: RxEvent): Unit
  def next: Rx[RxEvent]
  def stop(): Unit = add(OnCompletion)
