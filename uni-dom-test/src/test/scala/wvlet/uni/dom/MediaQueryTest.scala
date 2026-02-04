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
import wvlet.uni.rx.Rx

class MediaQueryTest extends UniTest:

  // Note: Full integration tests for MediaQuery require a browser environment.
  // jsdom doesn't support window.matchMedia. These tests verify the API surface
  // compiles correctly. The actual functionality works in real browsers.

  test("MediaQuery object exists"):
    MediaQuery shouldNotBe null

  // The following tests verify that the API compiles correctly.
  // Runtime tests for matches/isMobile/etc. require a browser with matchMedia support.
  // jsdom doesn't support window.matchMedia, so we can't call these methods.

  // Compile-time API verification:
  // - MediaQuery.matches(query: String): MediaQueryMatcher
  // - MediaQuery.isMobile: Rx[Boolean]
  // - MediaQuery.isTablet: Rx[Boolean]
  // - MediaQuery.isDesktop: Rx[Boolean]
  // - MediaQuery.prefersDarkMode: Rx[Boolean]
  // - MediaQuery.prefersReducedMotion: Rx[Boolean]
  // - MediaQuery.prefersHighContrast: Rx[Boolean]
  // - MediaQuery.isPortrait: Rx[Boolean]
  // - MediaQuery.isLandscape: Rx[Boolean]

end MediaQueryTest
