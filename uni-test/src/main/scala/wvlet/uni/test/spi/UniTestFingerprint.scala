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

import sbt.testing.SubclassFingerprint

/**
  * Fingerprint that identifies test classes extending UniTest
  */
object UniTestFingerprint extends SubclassFingerprint:
  override def superclassName(): String           = "wvlet.uni.test.UniTest"
  override def isModule(): Boolean                = false
  override def requireNoArgConstructor(): Boolean = true

/**
  * Fingerprint that identifies test objects (modules) extending UniTest
  */
object UniTestModuleFingerprint extends SubclassFingerprint:
  override def superclassName(): String           = "wvlet.uni.test.UniTest"
  override def isModule(): Boolean                = true
  override def requireNoArgConstructor(): Boolean = false
