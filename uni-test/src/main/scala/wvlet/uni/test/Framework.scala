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

import sbt.testing.Fingerprint
import sbt.testing.Runner

/**
  * sbt test framework entry point for uni-test
  */
class Framework extends sbt.testing.Framework:
  override def name(): String = "uni-test"

  override def fingerprints(): Array[Fingerprint] = Array(spi.UniTestFingerprint)

  override def runner(
      args: Array[String],
      remoteArgs: Array[String],
      testClassLoader: ClassLoader
  ): Runner = spi.UniTestRunner(args, remoteArgs, testClassLoader)

  /**
    * Scala.js slave runner - used for remote test execution. For now, we just create a standard
    * runner since we don't support distributed testing.
    */
  def slaveRunner(
      args: Array[String],
      remoteArgs: Array[String],
      testClassLoader: ClassLoader,
      send: String => Unit
  ): Runner = spi.UniTestRunner(args, remoteArgs, testClassLoader)
