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

import scala.collection.mutable.ListBuffer

/**
  * A mutable builder for constructing complex conditional Tailwind CSS class lists.
  *
  * TwClasses is useful when you need to build class strings based on multiple conditions or when the
  * logic is too complex for simple style composition.
  *
  * Usage:
  * {{{
  *   val classes = TwClasses()
  *     .add("flex items-center")
  *     .addIf(isLarge, "text-lg")
  *     .addUnless(isCompact, "gap-4")
  *     .addMatch(variant) {
  *       case "primary"   => "bg-blue-500"
  *       case "secondary" => "bg-gray-500"
  *       case _           => "bg-white"
  *     }
  *     .build()
  *
  *   div(classes, ...)
  * }}}
  */
class TwClasses:
  private val parts: ListBuffer[String] = ListBuffer.empty

  /**
    * Add classes unconditionally.
    */
  def add(classes: String): TwClasses =
    if classes.nonEmpty then parts += classes
    this

  /**
    * Add a TwStyle's classes unconditionally.
    */
  def add(style: TwStyle): TwClasses =
    if style.nonEmpty then parts += style.classes
    this

  /**
    * Add classes only if the condition is true.
    */
  def addIf(condition: Boolean, classes: String): TwClasses =
    if condition && classes.nonEmpty then parts += classes
    this

  /**
    * Add a TwStyle's classes only if the condition is true.
    */
  def addIf(condition: Boolean, style: TwStyle): TwClasses =
    if condition && style.nonEmpty then parts += style.classes
    this

  /**
    * Add classes only if the condition is false.
    */
  def addUnless(condition: Boolean, classes: String): TwClasses =
    if !condition && classes.nonEmpty then parts += classes
    this

  /**
    * Add a TwStyle's classes only if the condition is false.
    */
  def addUnless(condition: Boolean, style: TwStyle): TwClasses =
    if !condition && style.nonEmpty then parts += style.classes
    this

  /**
    * Add classes from an Option if present.
    */
  def addOption(opt: Option[String]): TwClasses =
    opt.foreach(s => if s.nonEmpty then parts += s)
    this

  /**
    * Add a TwStyle's classes from an Option if present.
    */
  def addOption(opt: Option[TwStyle]): TwClasses =
    opt.foreach(s => if s.nonEmpty then parts += s.classes)
    this

  /**
    * Add classes based on pattern matching.
    */
  def addMatch[T](value: T)(pf: PartialFunction[T, String]): TwClasses =
    if pf.isDefinedAt(value) then
      val classes = pf(value)
      if classes.nonEmpty then parts += classes
    this

  /**
    * Add a TwStyle based on pattern matching.
    */
  def addMatchStyle[T](value: T)(pf: PartialFunction[T, TwStyle]): TwClasses =
    if pf.isDefinedAt(value) then
      val style = pf(value)
      if style.nonEmpty then parts += style.classes
    this

  /**
    * Add classes with a variant prefix if the condition is true.
    */
  def addVariantIf(condition: Boolean, variant: String, classes: String): TwClasses =
    if condition && classes.nonEmpty then
      val prefixed = classes.split("\\s+").map(c => s"${variant}:${c}").mkString(" ")
      parts += prefixed
    this

  /**
    * Build the final TwStyle from accumulated classes.
    */
  def build(): TwStyle = TwStyle(parts.mkString(" "))

  /**
    * Get the current classes as a string (without creating a TwStyle).
    */
  def toClasses: String = parts.mkString(" ")

  /**
    * Check if any classes have been added.
    */
  def isEmpty: Boolean = parts.isEmpty

  /**
    * Check if classes have been added.
    */
  def nonEmpty: Boolean = parts.nonEmpty

  /**
    * Clear all accumulated classes.
    */
  def clear(): TwClasses =
    parts.clear()
    this

end TwClasses

object TwClasses:
  /**
    * Create a new empty TwClasses builder.
    */
  def apply(): TwClasses = new TwClasses

  /**
    * Create a TwClasses builder with initial classes.
    */
  def apply(initial: String): TwClasses =
    val builder = new TwClasses
    builder.add(initial)
    builder

  /**
    * Create a TwClasses builder with an initial TwStyle.
    */
  def apply(initial: TwStyle): TwClasses =
    val builder = new TwClasses
    builder.add(initial)
    builder

end TwClasses
