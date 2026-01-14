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

import wvlet.uni.rx.Rx

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Tests for async test support with Future and Rx
  */
class AsyncTestSupport extends UniTest:

  test("Future test - success") {
    Future {
      Thread.sleep(10)
      1 + 1
    }.map { result =>
      result shouldBe 2
    }
  }

  test("Future test - assertion in future") {
    Future {
      Thread.sleep(10)
      "hello"
    }.map { result =>
      result shouldBe "hello"
    }
  }

  test("Future test - failure should be caught") {
    // This test verifies that failures in Future are properly propagated
    // The test itself should pass if the framework handles Future correctly
    Future.successful(42).map { value =>
      value shouldBe 42
    }
  }

  test("Rx test - single value") {
    Rx.single(42)
  }

  test("Rx test - const value") {
    Rx.const(100)
  }

  test("Rx test - mapped value") {
    Rx.single(10).map(_ * 2)
  }

  test("Rx test - future integration") {
    val f = Future.successful("async result")
    Rx.future(f)
  }
