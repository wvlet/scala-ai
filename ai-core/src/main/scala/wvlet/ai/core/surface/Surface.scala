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

package wvlet.ai.core.surface

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*

/**
  * Note: This interface is the same with scala-2 Surface interface, but Scala compiler requires
  * defining Surface object in the same file, so this interface is copied.
  */
trait Surface extends Serializable:
  def rawType: Class[?]
  def typeArgs: Seq[Surface]
  def params: Seq[Parameter]
  def name: String
  def fullName: String

  def dealias: Surface = this

  def isOption: Boolean
  def isAlias: Boolean
  def isPrimitive: Boolean
  def isSeq: Boolean   = classOf[Seq[?]].isAssignableFrom(rawType)
  def isMap: Boolean   = classOf[Map[?, ?]].isAssignableFrom(rawType)
  def isArray: Boolean = this.isInstanceOf[ArraySurface]

  /**
    * True if this surface is a Scala3 enum
    */
  def isEnum: Boolean = classOf[scala.reflect.Enum].isAssignableFrom(rawType)

  def objectFactory: Option[ObjectFactory] = None
  def withOuter(outer: AnyRef): Surface    = this

/**
  * Scala 3 implementation of Surface
  */
object Surface:
  private[surface] val scalaMajorVersion: Int = 3

  import scala.quoted.*

  inline def of[A]: Surface =
    ${
      CompileTimeSurfaceFactory.surfaceOf[A]
    }

  inline def methodsOf[A]: Seq[MethodSurface] =
    ${
      CompileTimeSurfaceFactory.methodsOf[A]
    }

  val surfaceCache       = ConcurrentHashMap[String, Surface]().asScala
  val methodSurfaceCache = ConcurrentHashMap[String, Seq[MethodSurface]].asScala

  def getCached(fullName: String): Surface = surfaceCache(fullName)
