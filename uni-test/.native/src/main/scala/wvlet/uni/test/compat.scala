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

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
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
    * Create a new instance of the test class using Scala Native reflection. Throws exception if
    * class cannot be found or instantiated.
    */
  def newInstance(className: String, classLoader: ClassLoader): UniTest = Reflect
    .lookupInstantiatableClass(className)
    .map(_.newInstance().asInstanceOf[UniTest])
    .getOrElse(throw new ClassNotFoundException(s"Cannot find or instantiate: ${className}"))

  /**
    * Find the root cause of an exception.
    */
  def findCause(e: Throwable): Throwable = e

  /**
    * Await the result of an async test. Supports:
    * - scala.concurrent.Future[A]
    * - wvlet.uni.rx.RxOps[A] (detected via class name check)
    *
    * For synchronous results, returns the value as-is.
    */
  def awaitTestResult(result: Any): Any =
    result match
      case f: Future[?] =>
        // Await Future result
        Await.result(f, Duration.Inf)
      case _ =>
        // Check if result is RxOps using class name (Scala Native has limited reflection)
        val className = result.getClass.getName
        if className.startsWith("wvlet.uni.rx") then
          // Try to call the await method
          awaitRxResult(result)
        else
          // Return synchronous result as-is
          result

  /**
    * Await RxOps result. Uses the await method which should be available on RxOps.
    */
  private def awaitRxResult(rx: Any): Any =
    // In Scala Native, we need to use the runtime reflection available
    // For now, we rely on the RxOps having a well-known await method
    try
      val method = rx.getClass.getMethod("await")
      method.invoke(rx)
    catch
      case e: Exception =>
        throw new RuntimeException(s"Failed to await Rx result: ${e.getMessage}", e)

end compat
