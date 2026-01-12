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
package wvlet.uni.test

import scala.scalajs.js

/**
  * Scala.js specific compatibility layer for uni-test
  */
private[test] object compat:

  /**
    * Platform-specific equality check. Returns Some(result) if the comparison was handled, None if
    * the default comparison should be used.
    */
  def platformSpecificEquals(a: Any, b: Any): Option[Boolean] =
    (a, b) match
      case (obj1: js.Object, obj2: js.Object) =>
        Some(jsObjectEquals(obj1, obj2))
      case _ =>
        None

  /**
    * Deep equality check for JavaScript objects
    */
  private def jsObjectEquals(v1: js.Object, v2: js.Object): Boolean =
    if v1 eq v2 then
      true
    else if v1 == null || v2 == null then
      false
    else
      deepEqual(v1, v2)

  private def deepEqual(v1: js.Object, v2: js.Object): Boolean =
    val k1 = js.Object.keys(v1)
    val k2 = js.Object.keys(v2)

    if k1.length != k2.length then
      false
    else if k1.length == 0 then
      // For objects with no enumerable keys (e.g., RegExp, Error, Date),
      // JSON.stringify returns "{}" which is not useful for comparison.
      // Fall back to toString() comparison for such objects.
      val str1 = js.JSON.stringify(v1)
      val str2 = js.JSON.stringify(v2)
      if str1 == "{}" && str2 == "{}" then
        // Both stringify to empty object, use toString for comparison
        v1.asInstanceOf[js.Dynamic].toString() == v2.asInstanceOf[js.Dynamic].toString()
      else
        str1 == str2
    else
      // Get sorted keys and compare values for each key
      val sortedKeys = k1.toSeq.sorted
      sortedKeys.forall { key =>
        val jsVal1 = v1.asInstanceOf[js.Dynamic].selectDynamic(key)
        val jsVal2 = v2.asInstanceOf[js.Dynamic].selectDynamic(key)
        // Note: In JavaScript, typeof null === "object", so we must check null first
        val isNull1 = jsVal1 == null
        val isNull2 = jsVal2 == null
        if isNull1 && isNull2 then
          true
        else if isNull1 || isNull2 then
          false
        else if js.typeOf(jsVal1) == "object" && js.typeOf(jsVal2) == "object" then
          jsObjectEquals(jsVal1.asInstanceOf[js.Object], jsVal2.asInstanceOf[js.Object])
        else
          jsVal1 == jsVal2
      }

    end if

  end deepEqual

end compat
