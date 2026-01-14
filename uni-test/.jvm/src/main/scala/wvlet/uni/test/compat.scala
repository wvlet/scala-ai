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
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration

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
    * Create a new instance of the test class. Throws exception if instantiation fails.
    */
  def newInstance(className: String, classLoader: ClassLoader): UniTest =
    val testClass = classLoader.loadClass(className)
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

  /**
    * Await the result of an async test. Supports:
    * - scala.concurrent.Future[A]
    * - wvlet.uni.rx.RxOps[A] (detected via reflection to avoid compile-time dependency)
    *
    * For synchronous results, returns the value as-is.
    */
  def awaitTestResult(result: Any): Any =
    result match
      case f: Future[?] =>
        // Await Future result
        Await.result(f, Duration.Inf)
      case _ =>
        // Check if result is RxOps using reflection (to avoid compile-time dependency)
        if isRxOps(result) then
          // Call the await method on RxOps
          awaitRxResult(result)
        else
          // Return synchronous result as-is
          result

  /**
    * Check if the result is an RxOps instance using reflection
    */
  private def isRxOps(result: Any): Boolean =
    try
      val rxOpsClass = Class.forName("wvlet.uni.rx.RxOps")
      rxOpsClass.isInstance(result)
    catch
      case _: ClassNotFoundException =>
        false

  /**
    * Await RxOps result using reflection
    */
  private def awaitRxResult(rx: Any): Any =
    try
      // Call rx.await method
      val awaitMethod = rx.getClass.getMethod("await")
      awaitMethod.invoke(rx)
    catch
      case e: InvocationTargetException =>
        throw e.getCause
      case e: Exception =>
        throw new RuntimeException(s"Failed to await Rx result: ${e.getMessage}", e)

end compat
