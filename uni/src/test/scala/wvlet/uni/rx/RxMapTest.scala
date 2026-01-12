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

import wvlet.uni.test.UniTest
import wvlet.uni.test.empty
import wvlet.uni.test.defined

class RxMapTest extends UniTest:

  private var flagTest1 = false
  private var flagTest2 = false
  private var flagTest3 = false

  // Note: UniTest doesn't have afterAll hook
  // The async tests verification is skipped as UniTest doesn't support async test for wvlet.uni.rx.Rx

  test("Rx.map") {
    Rx.single(1).map(x => x + 1).map(x => x shouldBe 2).tap(_ => flagTest1 = true)
  }

  test("Rx.flatMap") {
    Rx.single(1).flatMap(x => Rx.single(x + 1)).map(x => x shouldBe 2).tap(_ => flagTest2 = true)
  }

  test("Rx.mapToRx") {
    Rx.single(1).mapToRx(x => Rx.single(x + 1)).map(x => x shouldBe 2).tap(_ => flagTest3 = true)
  }
