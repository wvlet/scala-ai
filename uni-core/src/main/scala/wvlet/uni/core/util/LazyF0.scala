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

object LazyF0:
  def apply[R](f: => R): LazyF0[R] = new LazyF0(f)

/**
  * This class is used to get the class names of the call-by-name functions (Function0[R]).
  *
  * This wrapper does not directly access the field f (Function0[R]) to avoid the evaluation of the
  * function body.
  * @param f
  * @tparam R
  */
class LazyF0[+R](f: => R) extends Serializable with Cloneable:
  // Generates an object ID to make sure the identity of this LazyF0 instance after serde
  private val objectId = new Object().hashCode()

  def copy: LazyF0[R] = clone().asInstanceOf[this.type]

  /**
    * This definition is necessary to let compiler generate the private field 'f' that holds a
    * reference to the call-by-name function.
    *
    * @return
    */
  def eval: R = f

  override def hashCode(): Int = objectId

  infix def canEqual(other: Any): Boolean = other.isInstanceOf[LazyF0[?]]

  override def equals(other: Any): Boolean =
    other match
      case that: LazyF0[?] =>
        // Scala 2.12 generates Lambda for Function0, and the class might be generated every time, so
        // comparing functionClasses doesn't work
        (that canEqual this) && this.objectId == that.objectId
      case _ =>
        false

end LazyF0
