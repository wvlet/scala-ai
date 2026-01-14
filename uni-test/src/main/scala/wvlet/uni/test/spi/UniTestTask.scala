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
import wvlet.uni.test.TestException
import wvlet.uni.test.TestResult
import wvlet.uni.test.TestSkipped
import wvlet.uni.test.TestPending
import wvlet.uni.test.TestCancelled
import wvlet.uni.test.TestIgnored
import wvlet.uni.test.UniTest
import wvlet.uni.test.compat

import scala.concurrent.Await
import scala.concurrent.Promise
import scala.concurrent.duration.Duration
import scala.io.AnsiColor

/**
  * ANSI color palette for test output, extending standard AnsiColor with additional bright colors
  */
private[spi] trait TestColorPalette extends AnsiColor:
  final val GRAY = "\u001b[90m"

/**
  * sbt test task that executes tests for a single test class
  */
class UniTestTask(_taskDef: TaskDef, testClassLoader: ClassLoader, config: TestConfig)
    extends Task
    with TestColorPalette:

  def taskDef(): TaskDef = _taskDef

  override def tags(): Array[String] = Array.empty

  /**
    * Synchronous execute method for JVM. Delegates to the async version using Promise/Await.
    */
  override def execute(
      eventHandler: EventHandler,
      loggers: Array[sbt.testing.Logger]
  ): Array[Task] =
    val p = Promise[Unit]()
    execute(eventHandler, loggers, _ => p.success(()))
    Await.result(p.future, Duration.Inf)
    Array.empty

  /**
    * Asynchronous execute method for Scala.js. This is the main implementation that both sync and
    * async paths use.
    */
  def execute(
      eventHandler: EventHandler,
      loggers: Array[sbt.testing.Logger],
      continuation: Array[Task] => Unit
  ): Unit =
    implicit val ec = compat.executionContext
    val className   = taskDef().fullyQualifiedName()

    try
      // Load the test class using platform-specific reflection
      val testInstance = compat.newInstance(className, testClassLoader)
      runTests(testInstance, className, eventHandler, loggers)
    catch
      case e: Throwable =>
        // Unwrap exception to get the actual cause
        val cause           = compat.findCause(e)
        val (event, logMsg) = classifySpecLevelException(className, cause)
        eventHandler.handle(event)
        loggers.foreach(_.info(logMsg))
        // Only trace for actual errors, not for skipped/pending/cancelled tests
        cause match
          case _: TestSkipped | _: TestPending | _: TestCancelled | _: TestIgnored =>
          // Don't trace for expected test control flow exceptions
          case _ =>
            loggers.foreach(_.trace(cause))
    finally
      continuation(Array.empty)
    end try

  end execute

  private def runTests(
      testInstance: UniTest,
      className: String,
      eventHandler: EventHandler,
      loggers: Array[sbt.testing.Logger]
  ): Unit =
    // Get initial tests and apply filter if specified
    val allTests      = testInstance.registeredTests
    val filteredTests =
      config.testFilter match
        case Some(filter) =>
          allTests.filter(_.fullName.contains(filter))
        case None =>
          allTests

    if filteredTests.isEmpty then
      // No tests registered via test() DSL, log info
      loggers.foreach(_.info(s"No tests found in ${className}"))
    else
      // Use queue-based approach to handle dynamically registered nested tests
      val testQueue     = scala.collection.mutable.Queue.from(filteredTests)
      val executedTests = scala.collection.mutable.Set.empty[String]

      while testQueue.nonEmpty do
        val testDef = testQueue.dequeue()
        if !executedTests.contains(testDef.fullName) then
          executedTests.add(testDef.fullName)

          val beforeCount = testInstance.registeredTests.size
          val result      = testInstance.executeTest(testDef)
          val isContainer = testInstance.registeredTests.size > beforeCount

          // Report failing containers or any leaf test
          if result.isFailure || !isContainer then
            val event = createEvent(testDef.fullName, result)
            eventHandler.handle(event)
            logResult(result, loggers)

          // Queue nested tests for execution (if container didn't fail)
          if !result.isFailure && isContainer then
            testInstance
              .registeredTests
              .foreach { t =>
                if !executedTests.contains(t.fullName) then
                  testQueue.enqueue(t)
              }
    end if

  end runTests

  private def logResult(result: TestResult, loggers: Array[sbt.testing.Logger]): Unit =
    result match
      case TestResult.Success(name) =>
        loggers.foreach(_.info(s"${GREEN}  + ${name}${RESET}"))
      case TestResult.Failure(name, msg, _) =>
        loggers.foreach(_.error(s"${RED}  - ${name}: ${msg}${RESET}"))
      case TestResult.Error(name, msg, cause) =>
        loggers.foreach(_.error(s"${RED}  x ${name}: ${msg}${RESET}"))
        loggers.foreach(_.trace(cause))
      case TestResult.Skipped(name, reason) =>
        loggers.foreach(_.info(s"${YELLOW}  ~ ${name}: skipped - ${reason}${RESET}"))
      case TestResult.Pending(name, reason) =>
        loggers.foreach(_.info(s"${MAGENTA}  ? ${name}: pending - ${reason}${RESET}"))
      case TestResult.Cancelled(name, reason) =>
        loggers.foreach(_.info(s"${CYAN}  ! ${name}: cancelled - ${reason}${RESET}"))
      case TestResult.Ignored(name, reason) =>
        loggers.foreach(_.info(s"${GRAY}  - ${name}: ignored - ${reason}${RESET}"))

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
      taskDef().fullyQualifiedName(),
      taskDef().fingerprint(),
      selector,
      status,
      throwable,
      0L
    )

  end createEvent

  private def createErrorEvent(className: String, e: Throwable): Event = UniTestEvent(
    className,
    taskDef().fingerprint(),
    new SuiteSelector(),
    Status.Error,
    new OptionalThrowable(e),
    0L
  )

  /**
    * Classify an exception thrown at the spec level (during class construction) and create an
    * appropriate event
    */
  private def classifySpecLevelException(className: String, e: Throwable): (Event, String) =
    val leafName = className.split('.').last
    e match
      case ts: TestSkipped =>
        val event = UniTestEvent(
          className,
          taskDef().fingerprint(),
          new SuiteSelector(),
          Status.Skipped,
          new OptionalThrowable(ts),
          0L
        )
        (event, s"${YELLOW}  ~ ${leafName}: skipped - ${ts.getMessage}${RESET}")
      case tp: TestPending =>
        val event = UniTestEvent(
          className,
          taskDef().fingerprint(),
          new SuiteSelector(),
          Status.Pending,
          new OptionalThrowable(tp),
          0L
        )
        (event, s"${MAGENTA}  ? ${leafName}: pending - ${tp.getMessage}${RESET}")
      case tc: TestCancelled =>
        val event = UniTestEvent(
          className,
          taskDef().fingerprint(),
          new SuiteSelector(),
          Status.Canceled,
          new OptionalThrowable(tc),
          0L
        )
        (event, s"${CYAN}  ! ${leafName}: cancelled - ${tc.getMessage}${RESET}")
      case ti: TestIgnored =>
        val event = UniTestEvent(
          className,
          taskDef().fingerprint(),
          new SuiteSelector(),
          Status.Ignored,
          new OptionalThrowable(ti),
          0L
        )
        (event, s"${GRAY}  - ${leafName}: ignored - ${ti.getMessage}${RESET}")
      case _ =>
        val event = UniTestEvent(
          className,
          taskDef().fingerprint(),
          new SuiteSelector(),
          Status.Error,
          new OptionalThrowable(e),
          0L
        )
        (event, s"${RED}  x ${leafName}: error - ${e.getMessage}${RESET}")

    end match

  end classifySpecLevelException

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
