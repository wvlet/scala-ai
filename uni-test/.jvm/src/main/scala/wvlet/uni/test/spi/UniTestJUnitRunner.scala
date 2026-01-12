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
package wvlet.uni.test.spi

import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier
import wvlet.uni.test.TestDef
import wvlet.uni.test.TestResult
import wvlet.uni.test.UniTest

/**
  * JUnit 4 Runner for UniTest.
  *
  * This enables reliable IDE integration (IntelliJ, VS Code) by implementing the standard JUnit 4
  * Runner interface. IDEs detect @RunWith annotations and use this runner to execute tests.
  *
  * This approach is used by munit and other modern test frameworks for maximum IDE compatibility.
  */
class UniTestJUnitRunner(testClass: Class[? <: UniTest]) extends Runner:

  override def getDescription: Description =
    val suiteDesc = Description.createSuiteDescription(testClass)
    // Create a temporary instance to discover test names
    try
      val instance = testClass.getDeclaredConstructor().newInstance()
      instance
        .registeredTests
        .foreach { testDef =>
          suiteDesc.addChild(createTestDescription(testDef))
        }
    catch
      case _: Throwable =>
      // If instantiation fails, return empty suite - error will be reported during run()
    suiteDesc

  override def run(notifier: RunNotifier): Unit =
    val suiteDesc = getDescription
    notifier.fireTestSuiteStarted(suiteDesc)

    try
      val instance = testClass.getDeclaredConstructor().newInstance()

      // Queue-based execution for nested tests (same pattern as UniTestEngine)
      val testQueue     = scala.collection.mutable.Queue.from(instance.registeredTests)
      val executedTests = scala.collection.mutable.Set.empty[String]

      while testQueue.nonEmpty do
        val testDef = testQueue.dequeue()
        if !executedTests.contains(testDef.fullName) then
          executedTests.add(testDef.fullName)

          val testDesc    = createTestDescription(testDef)
          val beforeCount = instance.registeredTests.size

          notifier.fireTestStarted(testDesc)
          val result      = instance.executeTest(testDef)
          val isContainer = instance.registeredTests.size > beforeCount

          // Report result for leaf tests or failing containers
          if result.isFailure || !isContainer then
            reportResult(notifier, testDesc, result)
          else
            // Container that passed - just mark as finished
            notifier.fireTestFinished(testDesc)

          // Queue nested tests for execution (if container didn't fail)
          if !result.isFailure && isContainer then
            instance
              .registeredTests
              .foreach { t =>
                if !executedTests.contains(t.fullName) then
                  testQueue.enqueue(t)
              }
        end if
      end while
    catch
      case e: Throwable =>
        // Report suite-level failure
        val failureDesc = Description.createTestDescription(testClass, "initialization")
        notifier.fireTestStarted(failureDesc)
        notifier.fireTestFailure(Failure(failureDesc, e))
        notifier.fireTestFinished(failureDesc)
    end try

    notifier.fireTestSuiteFinished(suiteDesc)

  end run

  private def createTestDescription(testDef: TestDef): Description = Description
    .createTestDescription(testClass, testDef.fullName)

  private def reportResult(notifier: RunNotifier, desc: Description, result: TestResult): Unit =
    result match
      case TestResult.Success(_) =>
        notifier.fireTestFinished(desc)
      case TestResult.Failure(_, msg, causeOpt) =>
        val throwable = causeOpt.getOrElse(AssertionError(msg))
        notifier.fireTestFailure(Failure(desc, throwable))
        notifier.fireTestFinished(desc)
      case TestResult.Error(_, _, cause) =>
        notifier.fireTestFailure(Failure(desc, cause))
        notifier.fireTestFinished(desc)
      case TestResult.Skipped(_, _) =>
        notifier.fireTestIgnored(desc)
      case TestResult.Pending(_, _) =>
        notifier.fireTestIgnored(desc)
      case TestResult.Cancelled(_, _) =>
        notifier.fireTestIgnored(desc)
      case TestResult.Ignored(_, _) =>
        notifier.fireTestIgnored(desc)

end UniTestJUnitRunner
