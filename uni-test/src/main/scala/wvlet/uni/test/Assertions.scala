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

import wvlet.uni.util.SourceCode
import scala.reflect.ClassTag

/**
  * Marker object for `shouldBe defined` check
  */
sealed trait DefinedMatcher
object defined extends DefinedMatcher

/**
  * Marker object for `shouldBe empty` check
  */
sealed trait EmptyMatcher
object empty extends EmptyMatcher

/**
  * Provides assertion methods for testing. Import or mix in this trait to use shouldBe,
  * shouldNotBe, etc.
  */
trait Assertions:

  extension [A](actual: A)
    /**
      * Assert that actual equals expected
      */
    inline infix def shouldBe(expected: A)(using source: SourceCode): Unit =
      if !Assertions.deepEquals(actual, expected) then
        throw AssertionFailure(s"Expected <${expected}> but got <${actual}>", source)

    /**
      * Assert that actual does not equal expected
      */
    inline infix def shouldNotBe(expected: A)(using source: SourceCode): Unit =
      if Assertions.deepEquals(actual, expected) then
        throw AssertionFailure(s"Expected not <${expected}> but got the same value", source)

    /**
      * Assert that option/collection is defined (non-empty)
      */
    inline infix def shouldBe(matcher: DefinedMatcher)(using source: SourceCode): Unit =
      val isDefined =
        actual match
          case opt: Option[?] =>
            opt.isDefined
          case seq: Iterable[?] =>
            seq.nonEmpty
          case null =>
            false
          case _ =>
            true
      if !isDefined then
        throw AssertionFailure(s"Expected defined but got <${actual}>", source)

    /**
      * Assert that option/collection is empty
      */
    inline infix def shouldBe(matcher: EmptyMatcher)(using source: SourceCode): Unit =
      val isEmpty =
        actual match
          case opt: Option[?] =>
            opt.isEmpty
          case seq: Iterable[?] =>
            seq.isEmpty
          case str: String =>
            str.isEmpty
          case null =>
            true
          case _ =>
            false
      if !isEmpty then
        throw AssertionFailure(s"Expected empty but got <${actual}>", source)

    /**
      * Assert that actual is the same instance as expected (reference equality)
      */
    inline infix def shouldBeTheSameInstanceAs(expected: A)(using source: SourceCode): Unit =
      if !(actual.asInstanceOf[AnyRef] eq expected.asInstanceOf[AnyRef]) then
        throw AssertionFailure(
          s"Expected same instance as <${expected}> but got different instance <${actual}>",
          source
        )

    /**
      * Assert that actual is not the same instance as expected
      */
    inline infix def shouldNotBeTheSameInstanceAs(expected: A)(using source: SourceCode): Unit =
      if actual.asInstanceOf[AnyRef] eq expected.asInstanceOf[AnyRef] then
        throw AssertionFailure(
          s"Expected different instance but got the same instance <${actual}>",
          source
        )

    /**
      * Assert that actual matches the given partial function
      */
    inline infix def shouldMatch(pf: PartialFunction[A, Any])(using source: SourceCode): Unit =
      if !pf.isDefinedAt(actual) then
        throw AssertionFailure(s"Value <${actual}> did not match the expected pattern", source)

    /**
      * Assert that option/collection is not defined (is empty)
      */
    inline infix def shouldNotBe(matcher: DefinedMatcher)(using source: SourceCode): Unit =
      val isDefined =
        actual match
          case opt: Option[?] =>
            opt.isDefined
          case seq: Iterable[?] =>
            seq.nonEmpty
          case null =>
            false
          case _ =>
            true
      if isDefined then
        throw AssertionFailure(s"Expected not defined but got <${actual}>", source)

    /**
      * Assert that option/collection is not empty
      */
    inline infix def shouldNotBe(matcher: EmptyMatcher)(using source: SourceCode): Unit =
      val isEmpty =
        actual match
          case opt: Option[?] =>
            opt.isEmpty
          case seq: Iterable[?] =>
            seq.isEmpty
          case str: String =>
            str.isEmpty
          case null =>
            true
          case _ =>
            false
      if !isEmpty then
        throw AssertionFailure(s"Expected not empty but got <${actual}>", source)

  end extension

  extension [A](actual: Iterable[A])
    /**
      * Assert that collection contains the element
      */
    inline infix def shouldContain(element: A)(using source: SourceCode): Unit =
      if !actual.exists(e => Assertions.deepEquals(e, element)) then
        throw AssertionFailure(s"Expected <${actual}> to contain <${element}>", source)

    /**
      * Assert that collection does not contain the element
      */
    inline infix def shouldNotContain(element: A)(using source: SourceCode): Unit =
      if actual.exists(e => Assertions.deepEquals(e, element)) then
        throw AssertionFailure(s"Expected <${actual}> not to contain <${element}>", source)

  extension (actual: String)
    /**
      * Assert that string contains the substring
      */
    inline infix def shouldContain(substring: String)(using source: SourceCode): Unit =
      if !actual.contains(substring) then
        throw AssertionFailure(s"Expected <${actual}> to contain <${substring}>", source)

    /**
      * Assert that string does not contain the substring
      */
    inline infix def shouldNotContain(substring: String)(using source: SourceCode): Unit =
      if actual.contains(substring) then
        throw AssertionFailure(s"Expected <${actual}> not to contain <${substring}>", source)

  /**
    * Assert that the given block throws an exception of type E
    */
  inline def intercept[E <: Throwable](body: => Any)(using ct: ClassTag[E], source: SourceCode): E =
    try
      body
      throw AssertionFailure(
        s"Expected exception of type ${ct.runtimeClass.getName} but no exception was thrown",
        source
      )
    catch
      case e: E if ct.runtimeClass.isInstance(e) =>
        e
      case e: AssertionFailure =>
        throw e
      case e: Throwable =>
        throw AssertionFailure(
          s"Expected exception of type ${ct.runtimeClass.getName} but got ${e.getClass.getName}: ${e
              .getMessage}",
          source
        )

  /**
    * Assert equality of floating point numbers with a delta tolerance
    */
  inline def assertEquals(actual: Float, expected: Float, delta: Float)(using
      source: SourceCode
  ): Unit =
    if Math.abs(actual - expected) > delta then
      throw AssertionFailure(s"Expected <${expected}> +/- ${delta} but got <${actual}>", source)

  /**
    * Assert equality of double numbers with a delta tolerance
    */
  inline def assertEquals(actual: Double, expected: Double, delta: Double)(using
      source: SourceCode
  ): Unit =
    if Math.abs(actual - expected) > delta then
      throw AssertionFailure(s"Expected <${expected}> +/- ${delta} but got <${actual}>", source)

  /**
    * Assert that a condition is true
    */
  inline def assert(cond: => Boolean)(using source: SourceCode): Unit =
    if !cond then
      throw AssertionFailure("Assertion failed", source)

  /**
    * Assert that a condition is true with a custom message
    */
  inline def assert(cond: => Boolean, message: => String)(using source: SourceCode): Unit =
    if !cond then
      throw AssertionFailure(message, source)

end Assertions

object Assertions:
  /**
    * Deep equality check that handles arrays, options, and collections
    */
  def deepEquals(a: Any, b: Any): Boolean =
    // First check platform-specific equality (e.g., js.Object in Scala.js)
    compat.platformSpecificEquals(a, b) match
      case Some(result) =>
        result
      case None =>
        // Fall back to standard deep equality
        (a, b) match
          case (null, null) =>
            true
          case (null, _) =>
            false
          case (_, null) =>
            false
          case (a1: Array[?], a2: Array[?]) =>
            a1.length == a2.length && a1.zip(a2).forall((x, y) => deepEquals(x, y))
          case (o1: Option[?], o2: Option[?]) =>
            (o1, o2) match
              case (Some(v1), Some(v2)) =>
                deepEquals(v1, v2)
              case (None, None) =>
                true
              case _ =>
                false
          case (s1: Seq[?], s2: Seq[?]) =>
            s1.length == s2.length && s1.zip(s2).forall((x, y) => deepEquals(x, y))
          case (s1: Set[?], s2: Set[?]) =>
            s1.size == s2.size && s1.forall(x => s2.exists(y => deepEquals(x, y)))
          case (m1: Map[?, ?], m2: Map[?, ?]) =>
            m1.size == m2.size &&
            m1.forall { case (k, v) =>
              m2.asInstanceOf[Map[Any, Any]].get(k).exists(v2 => deepEquals(v, v2))
            }
          case _ =>
            a == b

end Assertions
