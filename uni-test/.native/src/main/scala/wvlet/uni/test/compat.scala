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

import scala.concurrent.ExecutionContext
import scala.scalanative.reflect.Reflect

/**
  * Scala Native specific compatibility layer for uni-test
  */
private[test] object compat:

  /**
    * Platform-specific matcher for equality checks. On Native, there's no special handling needed,
    * so we return an empty PartialFunction.
    */
  def platformSpecificMatcher: PartialFunction[(Any, Any), MatchResult] = PartialFunction.empty

  /**
    * Execution context for async operations
    */
  val executionContext: ExecutionContext = ExecutionContext.global

  /**
    * Create a new instance of the test class or get singleton instance if it's an object. Uses
    * Scala Native reflection. Throws exception if class cannot be found or instantiated.
    */
  def newInstance(className: String, classLoader: ClassLoader): UniTest =
    // First try to look up as a loadable module (Scala object)
    Reflect
      .lookupLoadableModuleClass(className)
      .map(_.loadModule().asInstanceOf[UniTest])
      .orElse(
        // Fall back to instantiatable class (regular class)
        Reflect
          .lookupInstantiatableClass(className)
          .map(_.newInstance().asInstanceOf[UniTest])
      )
      .getOrElse(throw new ClassNotFoundException(s"Cannot find or instantiate: ${className}"))

  /**
    * Find the root cause of an exception.
    */
  def findCause(e: Throwable): Throwable = e

end compat
