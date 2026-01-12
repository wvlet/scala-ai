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

import wvlet.uni.log.Platform

/**
  * Provides test control flow methods: fail, skip, pending, cancel
  */
trait TestControl:

  // Platform detection helpers
  def isScalaJS: Boolean     = Platform.isScalaJS
  def isScalaNative: Boolean = Platform.isScalaNative
  def isScalaJVM: Boolean    = !isScalaJS && !isScalaNative
  def isScala3: Boolean      = true // uni only supports Scala 3

  /**
    * Run a block of code only for flaky tests. Failures are converted to skipped.
    */
  inline def flaky[A](body: => A)(using source: TestSource): A =
    try
      body
    catch
      case e: TestException =>
        throw e
      case e: Throwable =>
        throw TestSkipped(s"[flaky] ${e.getMessage}", source)

  /**
    * Explicitly fail the test with a message
    */
  inline def fail(message: String)(using source: TestSource): Nothing =
    throw AssertionFailure(message, source)

  /**
    * Skip the test with a reason. Use this for tests that should not run in certain environments.
    */
  inline def skip(message: String)(using source: TestSource): Nothing =
    throw TestSkipped(message, source)

  /**
    * Mark the test as pending. Use this for tests that are not yet implemented.
    */
  inline def pending(message: String)(using source: TestSource): Nothing =
    throw TestPending(message, source)

  /**
    * Mark the test as pending until the specified condition is fixed.
    */
  inline def pendingUntil(message: String)(using source: TestSource): Nothing =
    throw TestPending(s"Pending until: ${message}", source)

  /**
    * Cancel the test. Use this when setup fails and the test cannot be run.
    */
  inline def cancel(message: String)(using source: TestSource): Nothing =
    throw TestCancelled(message, source)

  /**
    * Ignore the test with a reason. Similar to skip but semantically different.
    */
  inline def ignore(message: String)(using source: TestSource): Nothing =
    throw TestIgnored(message, source)

end TestControl
