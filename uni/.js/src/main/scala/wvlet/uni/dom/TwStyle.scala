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

import wvlet.uni.rx.Rx

/**
  * A composable Tailwind CSS style definition that can be used as a DomNode.
  *
  * TwStyle provides a type-safe, composable way to work with Tailwind CSS classes. Styles can be
  * combined using the `+` operator and support variants for responsive design, dark mode, and
  * interactive states.
  *
  * Usage:
  * {{{
  *   import wvlet.uni.dom.all.*
  *
  *   // Basic usage
  *   div(tw("flex items-center gap-4"))
  *
  *   // Composing styles
  *   val buttonBase = tw("px-4 py-2 rounded font-medium")
  *   val buttonPrimary = tw("bg-blue-500 text-white hover:bg-blue-600")
  *   button(buttonBase + buttonPrimary, "Click me")
  *
  *   // With variants
  *   div(tw("bg-white").dark("bg-gray-900").hover("bg-gray-100"))
  * }}}
  */
case class TwStyle(classes: String) extends DomNode:

  /**
    * Combine this style with another style.
    */
  def +(other: TwStyle): TwStyle =
    if classes.isEmpty then other
    else if other.classes.isEmpty then this
    else TwStyle(s"${classes} ${other.classes}")

  /**
    * Combine this style with multiple other styles.
    */
  def ++(others: TwStyle*): TwStyle = others.foldLeft(this)(_ + _)

  /**
    * Add classes with a hover variant prefix.
    */
  def hover(cls: String): TwStyle = addVariant("hover", cls)

  /**
    * Add classes with a focus variant prefix.
    */
  def focus(cls: String): TwStyle = addVariant("focus", cls)

  /**
    * Add classes with an active variant prefix.
    */
  def active(cls: String): TwStyle = addVariant("active", cls)

  /**
    * Add classes with a disabled variant prefix.
    */
  def disabled(cls: String): TwStyle = addVariant("disabled", cls)

  /**
    * Add classes with a group-hover variant prefix.
    */
  def groupHover(cls: String): TwStyle = addVariant("group-hover", cls)

  /**
    * Add classes with a focus-within variant prefix.
    */
  def focusWithin(cls: String): TwStyle = addVariant("focus-within", cls)

  /**
    * Add classes with a focus-visible variant prefix.
    */
  def focusVisible(cls: String): TwStyle = addVariant("focus-visible", cls)

  /**
    * Add classes with a dark mode variant prefix.
    */
  def dark(cls: String): TwStyle = addVariant("dark", cls)

  /**
    * Add classes with a small breakpoint variant prefix (640px+).
    */
  def sm(cls: String): TwStyle = addVariant("sm", cls)

  /**
    * Add classes with a medium breakpoint variant prefix (768px+).
    */
  def md(cls: String): TwStyle = addVariant("md", cls)

  /**
    * Add classes with a large breakpoint variant prefix (1024px+).
    */
  def lg(cls: String): TwStyle = addVariant("lg", cls)

  /**
    * Add classes with an extra-large breakpoint variant prefix (1280px+).
    */
  def xl(cls: String): TwStyle = addVariant("xl", cls)

  /**
    * Add classes with a 2xl breakpoint variant prefix (1536px+).
    */
  def xxl(cls: String): TwStyle = addVariant("2xl", cls)

  /**
    * Add classes with a first-child variant prefix.
    */
  def first(cls: String): TwStyle = addVariant("first", cls)

  /**
    * Add classes with a last-child variant prefix.
    */
  def last(cls: String): TwStyle = addVariant("last", cls)

  /**
    * Add classes with an odd-child variant prefix.
    */
  def odd(cls: String): TwStyle = addVariant("odd", cls)

  /**
    * Add classes with an even-child variant prefix.
    */
  def even(cls: String): TwStyle = addVariant("even", cls)

  /**
    * Add classes with a custom variant prefix.
    */
  def variant(prefix: String, cls: String): TwStyle = addVariant(prefix, cls)

  /**
    * Check if this style is empty (no classes).
    */
  def isEmpty: Boolean = classes.isEmpty

  /**
    * Check if this style is non-empty.
    */
  def nonEmpty: Boolean = classes.nonEmpty

  private def addVariant(prefix: String, cls: String): TwStyle =
    if cls.isEmpty then this
    else
      val prefixed = cls.split("\\s+").map(c => s"${prefix}:${c}").mkString(" ")
      if classes.isEmpty then TwStyle(prefixed)
      else TwStyle(s"${classes} ${prefixed}")

end TwStyle

object TwStyle:
  /**
    * An empty TwStyle with no classes.
    */
  val empty: TwStyle = TwStyle("")

  /**
    * Create a TwStyle from a class string.
    */
  def apply(classes: String): TwStyle = new TwStyle(classes.trim)

  /**
    * Create a TwStyle from multiple class strings.
    */
  def apply(first: String, rest: String*): TwStyle =
    TwStyle((first +: rest).mkString(" ").trim)

  /**
    * Conditionally apply a style when the condition is true.
    */
  def when(condition: Boolean)(classes: String): TwStyle =
    if condition then TwStyle(classes) else empty

  /**
    * Conditionally apply a style based on a reactive condition.
    */
  def when(condition: Rx[Boolean])(classes: String): Rx[TwStyle] =
    condition.map(c => if c then TwStyle(classes) else empty)

  /**
    * Conditionally apply a style when the condition is false.
    */
  def unless(condition: Boolean)(classes: String): TwStyle =
    if !condition then TwStyle(classes) else empty

  /**
    * Conditionally apply a style based on a reactive condition being false.
    */
  def unless(condition: Rx[Boolean])(classes: String): Rx[TwStyle] =
    condition.map(c => if !c then TwStyle(classes) else empty)

  /**
    * Toggle between two styles based on a condition.
    */
  def toggle(condition: Boolean, whenTrue: String, whenFalse: String): TwStyle =
    if condition then TwStyle(whenTrue) else TwStyle(whenFalse)

  /**
    * Toggle between two styles based on a reactive condition.
    */
  def toggle(condition: Rx[Boolean], whenTrue: String, whenFalse: String): Rx[TwStyle] =
    condition.map(c => if c then TwStyle(whenTrue) else TwStyle(whenFalse))

  /**
    * Create a style from an optional class string.
    */
  def fromOption(opt: Option[String]): TwStyle =
    opt.map(TwStyle(_)).getOrElse(empty)

  /**
    * Merge multiple optional styles into one.
    */
  def merge(styles: Option[TwStyle]*): TwStyle =
    styles.flatten.foldLeft(empty)(_ + _)

end TwStyle

/**
  * Convenience function to create a TwStyle.
  *
  * Usage:
  * {{{
  *   div(tw("flex items-center"))
  *   button(tw("px-4 py-2") + tw("bg-blue-500"))
  * }}}
  */
def tw(classes: String): TwStyle = TwStyle(classes)

/**
  * Convenience function to create a TwStyle from multiple class strings.
  */
def tw(first: String, rest: String*): TwStyle = TwStyle(first, rest*)
