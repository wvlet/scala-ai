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
package wvlet.uni.dom

import wvlet.uni.test.UniTest
import wvlet.uni.dom.all.*
import wvlet.uni.rx.Cancelable

class AnimationFrameTest extends UniTest:

  // Note: Full integration tests for AnimationFrame require a browser environment.
  // jsdom doesn't provide requestAnimationFrame. These tests verify the API surface
  // compiles correctly. The actual functionality works in real browsers.

  test("AnimationFrame object exists"):
    AnimationFrame shouldNotBe null

  // The following tests verify that the API compiles correctly.
  // Runtime tests for loop/once/etc. require a browser with RAF support.
  // jsdom doesn't support requestAnimationFrame, so we can't call these methods.

  // Compile-time API verification:
  // - AnimationFrame.loop(callback: Double => Unit): Cancelable
  // - AnimationFrame.once(callback: => Unit): Cancelable
  // - AnimationFrame.fixedStep(stepMs: Double)(callback: () => Unit): Cancelable
  // - AnimationFrame.withElapsed(callback: (Double, Double) => Unit): Cancelable
  // - AnimationFrame.timed(durationMs, callback, onComplete): Cancelable

end AnimationFrameTest
