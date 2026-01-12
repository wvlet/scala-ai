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
  * Base class for all test-related exceptions
  */
sealed abstract class TestException(message: String, val source: TestSource)
    extends Exception(s"${message} (${source.fileLocation})")

/**
  * Thrown when an assertion fails
  */
class AssertionFailure(message: String, source: TestSource) extends TestException(message, source)

/**
  * Thrown when a test is skipped
  */
class TestSkipped(message: String, source: TestSource) extends TestException(message, source)

/**
  * Thrown when a test is marked as pending
  */
class TestPending(message: String, source: TestSource) extends TestException(message, source)

/**
  * Thrown when a test is cancelled (e.g., setup failure)
  */
class TestCancelled(message: String, source: TestSource) extends TestException(message, source)

/**
  * Thrown when a test is ignored
  */
class TestIgnored(message: String, source: TestSource) extends TestException(message, source)
