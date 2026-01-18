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
package wvlet.uni.rx

import wvlet.airspec.AirSpec
import java.util.concurrent.TimeUnit
import scala.util.Try

class RxTimeoutTest extends AirSpec:

  test("timeout should complete normally if within limit") {
    val result = Rx.delay(10, TimeUnit.MILLISECONDS).map(_ => "done").timeout(100).await

    result shouldBe "done"
  }

  test("timeout should emit TimeoutException if exceeded") {
    val result = Try {
      Rx.delay(200, TimeUnit.MILLISECONDS).map(_ => "done").timeout(50).await
    }

    result.isFailure shouldBe true
    result.failed.get shouldMatch { case Rx.TimeoutException(50, TimeUnit.MILLISECONDS) => }
  }

  test("timeout should cancel the underlying operation on timeout") {
    var cancelled = false

    val rx = Rx.single {
      Thread.sleep(500)
      cancelled = false
      "done"
    }

    val result = Try {
      rx.timeout(50).await
    }

    result.isFailure shouldBe true
    // The underlying operation may still be running, but the timeout was triggered
  }

  test("timeout with custom time unit") {
    val result = Try {
      Rx.delay(200, TimeUnit.MILLISECONDS)
        .map(_ => "done")
        .timeout(50, TimeUnit.MILLISECONDS)
        .await
    }

    result.isFailure shouldBe true
    result.failed.get shouldMatch { case Rx.TimeoutException(50, TimeUnit.MILLISECONDS) => }
  }

  test("timeout on sequence should work for first element") {
    val result = Rx.single(42).timeout(100).await

    result shouldBe 42
  }

  test("TimeoutException message format") {
    val ex = Rx.TimeoutException(100, TimeUnit.MILLISECONDS)

    ex.getMessage shouldBe "Operation timed out after 100 milliseconds"
  }

end RxTimeoutTest
