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

/**
  * Scala Native specific compatibility layer for uni-test
  */
private[test] object compat:

  /**
    * Platform-specific equality check. Returns Some(result) if the comparison was handled, None if
    * the default comparison should be used.
    *
    * On Native, there's no special handling needed, so we always return None.
    */
  def platformSpecificEquals(a: Any, b: Any): Option[Boolean] = None

end compat
