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

import java.lang.reflect.InvocationTargetException
import scala.concurrent.ExecutionContext

/**
  * JVM specific compatibility layer for uni-test
  */
private[test] object compat:

  /**
    * Platform-specific matcher for equality checks. On JVM, there's no special handling needed, so
    * we return an empty PartialFunction.
    */
  def platformSpecificMatcher: PartialFunction[(Any, Any), MatchResult] = PartialFunction.empty

  /**
    * Execution context for async operations
    */
  val executionContext: ExecutionContext = ExecutionContext.global

  /**
    * Create a new instance of the test class or get singleton instance if it's an object. Throws
    * exception if instantiation fails.
    */
  def newInstance(className: String, classLoader: ClassLoader): UniTest =
    val testClass = classLoader.loadClass(className)
    getInstanceOf(testClass)

  /**
    * Get an instance from a class. For Scala objects (modules), retrieves the singleton instance
    * via MODULE$ field. For regular classes, creates a new instance via no-arg constructor.
    */
  def getInstanceOf(testClass: Class[?]): UniTest =
    // Check if it's a Scala object (module) by looking for MODULE$ field
    try
      val moduleField = testClass.getField("MODULE$")
      moduleField.get(null).asInstanceOf[UniTest]
    catch
      case _: NoSuchFieldException =>
        // Not a module, create instance via constructor
        testClass.getDeclaredConstructor().newInstance().asInstanceOf[UniTest]

  /**
    * Unwrap InvocationTargetException and other wrapper exceptions to find the root cause
    */
  def findCause(e: Throwable): Throwable =
    e match
      case ite: InvocationTargetException if ite.getCause != null =>
        findCause(ite.getCause)
      case _ =>
        e

end compat
