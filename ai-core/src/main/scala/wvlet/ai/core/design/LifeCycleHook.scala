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
package wvlet.ai.core.design

import wvlet.ai.core.log.LogSupport
import wvlet.ai.core.typeshape.TypeShape

class Injectee(val typeShape: TypeShape, val injectee: Any):
  infix def canEqual(other: Any): Boolean = other.isInstanceOf[Injectee]
  override def equals(other: Any): Boolean =
    other match
      case that: Injectee =>
        (that canEqual this) && typeShape == that.typeShape && injectee == that.injectee
      case _ =>
        false

  override def hashCode(): Int =
    val h =
      31 * typeShape.hashCode() +
        (if injectee != null then
           injectee.hashCode()
         else
           0)
    h

trait LifeCycleHook:
  def typeShape: TypeShape = injectee.typeShape
  def execute: Unit
  def injectee: Injectee

object EventHookHolder:
  def apply[A](typeShape: TypeShape, injectee: A, hook: A => Any): EventHookHolder[A] =
    EventHookHolder(new Injectee(typeShape, injectee), hook)

case class EventHookHolder[A](injectee: Injectee, hook: A => Any)
    extends LifeCycleHook
    with LogSupport:
  override def toString: String = s"hook for [$typeShape]"
  def execute: Unit             = hook(injectee.injectee.asInstanceOf[A])
