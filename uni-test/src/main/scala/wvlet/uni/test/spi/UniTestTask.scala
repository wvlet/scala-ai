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

import sbt.testing.*
import wvlet.uni.test.TestResult
import wvlet.uni.test.UniTest

/**
  * sbt test task that executes tests for a single test class
  */
class UniTestTask(val taskDef: TaskDef, testClassLoader: ClassLoader, config: TestConfig)
    extends Task:

  override def tags(): Array[String] = Array.empty

  override def execute(
      eventHandler: EventHandler,
      loggers: Array[sbt.testing.Logger]
  ): Array[Task] =
    val className = taskDef.fullyQualifiedName()

    try
      // Load the test class
      val testClass    = testClassLoader.loadClass(className)
      val testInstance = testClass.getDeclaredConstructor().newInstance().asInstanceOf[UniTest]

      // Run all registered tests
      val tests = testInstance.registeredTests

      if tests.isEmpty then
        // No tests registered via test() DSL, log info
        loggers.foreach(_.info(s"No tests found in ${className}"))
      else
        for testDef <- tests do
          val result = testInstance.executeTest(testDef)
          val event  = createEvent(testDef.fullName, result)
          eventHandler.handle(event)

          // Log the result
          result match
            case TestResult.Success(name) =>
              loggers.foreach(_.info(s"  + ${name}"))
            case TestResult.Failure(name, msg, _) =>
              loggers.foreach(_.error(s"  - ${name}: ${msg}"))
            case TestResult.Error(name, msg, cause) =>
              loggers.foreach(_.error(s"  x ${name}: ${msg}"))
              loggers.foreach(_.trace(cause))
            case TestResult.Skipped(name, reason) =>
              loggers.foreach(_.info(s"  ~ ${name}: skipped - ${reason}"))
            case TestResult.Pending(name, reason) =>
              loggers.foreach(_.info(s"  ? ${name}: pending - ${reason}"))
            case TestResult.Cancelled(name, reason) =>
              loggers.foreach(_.info(s"  ! ${name}: cancelled - ${reason}"))
            case TestResult.Ignored(name, reason) =>
              loggers.foreach(_.info(s"  - ${name}: ignored - ${reason}"))

    catch
      case e: Throwable =>
        val event = createErrorEvent(className, e)
        eventHandler.handle(event)
        loggers.foreach(_.error(s"Failed to run tests in ${className}: ${e.getMessage}"))
        loggers.foreach(_.trace(e))
    end try

    // No nested tasks
    Array.empty

  end execute

  private def createEvent(testName: String, result: TestResult): Event =
    val selector = new TestSelector(testName)
    val status   =
      result match
        case TestResult.Success(_) =>
          Status.Success
        case TestResult.Failure(_, _, _) =>
          Status.Failure
        case TestResult.Error(_, _, _) =>
          Status.Error
        case TestResult.Skipped(_, _) =>
          Status.Skipped
        case TestResult.Pending(_, _) =>
          Status.Pending
        case TestResult.Cancelled(_, _) =>
          Status.Canceled
        case TestResult.Ignored(_, _) =>
          Status.Ignored

    val throwable =
      result match
        case TestResult.Failure(_, _, Some(e)) =>
          new OptionalThrowable(e)
        case TestResult.Error(_, _, e) =>
          new OptionalThrowable(e)
        case _ =>
          new OptionalThrowable()

    UniTestEvent(
      taskDef.fullyQualifiedName(),
      taskDef.fingerprint(),
      selector,
      status,
      throwable,
      0L
    )

  end createEvent

  private def createErrorEvent(className: String, e: Throwable): Event = UniTestEvent(
    className,
    taskDef.fingerprint(),
    new SuiteSelector(),
    Status.Error,
    new OptionalThrowable(e),
    0L
  )

end UniTestTask

/**
  * Event implementation for sbt test interface
  */
class UniTestEvent(
    _fullyQualifiedName: String,
    _fingerprint: Fingerprint,
    _selector: Selector,
    _status: Status,
    _throwable: OptionalThrowable,
    _duration: Long
) extends Event:
  override def fullyQualifiedName(): String   = _fullyQualifiedName
  override def fingerprint(): Fingerprint     = _fingerprint
  override def selector(): Selector           = _selector
  override def status(): Status               = _status
  override def throwable(): OptionalThrowable = _throwable
  override def duration(): Long               = _duration
