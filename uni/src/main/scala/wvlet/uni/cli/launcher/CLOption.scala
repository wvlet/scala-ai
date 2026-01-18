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
package wvlet.uni.cli.launcher

import wvlet.uni.surface.{Parameter, Primitive, Surface}

/**
  * Base trait for command-line option items
  */
sealed trait CLOptionItem:
  def param: Option[Parameter]
  def description: String

  /**
    * Whether this option takes an argument value
    */
  def takesArgument: Boolean

  /**
    * Whether this option can accept multiple values (for Seq, Array, etc.)
    */
  def takesMultipleArguments: Boolean

  /**
    * Whether this is a key-value option like -Dkey=value
    */
  def isKeyValue: Boolean

/**
  * Represents a command-line option with prefix(es) like -v, --verbose
  *
  * @param prefixes
  *   Option prefixes (e.g., Seq("-v", "--verbose"))
  * @param description
  *   Description for help message
  * @param param
  *   Associated parameter, None for built-in options like help
  */
case class CLOption(prefixes: Seq[String], description: String, param: Option[Parameter])
    extends CLOptionItem:

  /**
    * Primary prefix (first one, typically the short form)
    */
  def prefix: String = prefixes.headOption.getOrElse("")

  override def takesArgument: Boolean =
    param match
      case None =>
        false // Built-in flags like help
      case Some(p) =>
        val s           = p.surface
        val typeSurface =
          if s.isOption then
            s.typeArgs.headOption.getOrElse(s)
          else
            s
        typeSurface != Primitive.Boolean

  override def takesMultipleArguments: Boolean = param.exists(p =>
    p.surface.isSeq || p.surface.isArray
  )

  override def isKeyValue: Boolean = param.exists { p =>
    val s         = p.surface
    val innerType =
      if s.isOption then
        s.typeArgs.headOption.getOrElse(s)
      else if s.isSeq || s.isArray then
        s.typeArgs.headOption.getOrElse(s)
      else
        s
    innerType.fullName == "wvlet.uni.cli.launcher.KeyValue"
  }

  /**
    * Format for help display (e.g., "-v, --verbose")
    */
  def prefixString: String =
    if isKeyValue then
      s"${prefix}<key>=<value>"
    else
      prefixes.mkString(", ")

end CLOption

/**
  * Represents a positional argument
  *
  * @param index
  *   Position index (0-based)
  * @param name
  *   Display name for help
  * @param description
  *   Description for help message
  * @param param
  *   Associated parameter
  */
case class CLArgument(index: Int, name: String, description: String, param: Option[Parameter])
    extends CLOptionItem:

  override def takesArgument: Boolean = true

  override def takesMultipleArguments: Boolean = param.exists(p =>
    p.surface.isSeq || p.surface.isArray
  )

  override def isKeyValue: Boolean = false
