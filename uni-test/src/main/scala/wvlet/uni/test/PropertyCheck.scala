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

import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.Prop
import org.scalacheck.Test
import org.scalacheck.Test.Parameters
import wvlet.uni.util.SourceCode

/**
  * Property-based testing support using ScalaCheck. Mix in this trait with UniTest to use forAll
  * method for property-based testing.
  *
  * Example:
  * {{{
  *   class MyTest extends UniTest with PropertyCheck:
  *     test("property test") {
  *       forAll { (a: Int, b: Int) =>
  *         (a + b) shouldBe (b + a)
  *       }
  *     }
  * }}}
  */
trait PropertyCheck:
  private val testParameters: Parameters = Parameters.default.withMinSuccessfulTests(100)

  /**
    * Run a property check with implicit Arbitrary generators for a single parameter.
    */
  inline def forAll[A](body: A => Unit)(using arb: Arbitrary[A]): Unit =
    val prop = Prop.forAll { (a: A) =>
      body(a)
      true
    }
    checkProperty(prop)

  /**
    * Run a property check with implicit Arbitrary generators for two parameters.
    */
  inline def forAll[A, B](
      body: (A, B) => Unit
  )(using arbA: Arbitrary[A], arbB: Arbitrary[B]): Unit =
    val prop = Prop.forAll { (a: A, b: B) =>
      body(a, b)
      true
    }
    checkProperty(prop)

  /**
    * Run a property check with implicit Arbitrary generators for three parameters.
    */
  inline def forAll[A, B, C](
      body: (A, B, C) => Unit
  )(using arbA: Arbitrary[A], arbB: Arbitrary[B], arbC: Arbitrary[C]): Unit =
    val prop = Prop.forAll { (a: A, b: B, c: C) =>
      body(a, b, c)
      true
    }
    checkProperty(prop)

  /**
    * Run a property check with implicit Arbitrary generators for four parameters.
    */
  inline def forAll[A, B, C, D](
      body: (A, B, C, D) => Unit
  )(using arbA: Arbitrary[A], arbB: Arbitrary[B], arbC: Arbitrary[C], arbD: Arbitrary[D]): Unit =
    val prop = Prop.forAll { (a: A, b: B, c: C, d: D) =>
      body(a, b, c, d)
      true
    }
    checkProperty(prop)

  /**
    * Run a property check with implicit Arbitrary generators for five parameters.
    */
  inline def forAll[A, B, C, D, E](body: (A, B, C, D, E) => Unit)(using
      arbA: Arbitrary[A],
      arbB: Arbitrary[B],
      arbC: Arbitrary[C],
      arbD: Arbitrary[D],
      arbE: Arbitrary[E]
  ): Unit =
    val prop = Prop.forAll { (a: A, b: B, c: C, d: D, e: E) =>
      body(a, b, c, d, e)
      true
    }
    checkProperty(prop)

  /**
    * Run a property check with an explicit generator for a single parameter.
    */
  inline def forAll[A](gen: Gen[A])(body: A => Unit): Unit =
    val prop =
      Prop.forAll(gen) { (a: A) =>
        body(a)
        true
      }
    checkProperty(prop)

  /**
    * Run a property check with explicit generators for two parameters.
    */
  inline def forAll[A, B](genA: Gen[A], genB: Gen[B])(body: (A, B) => Unit): Unit =
    val prop =
      Prop.forAll(genA, genB) { (a: A, b: B) =>
        body(a, b)
        true
      }
    checkProperty(prop)

  /**
    * Run a property check with explicit generators for three parameters.
    */
  inline def forAll[A, B, C](genA: Gen[A], genB: Gen[B], genC: Gen[C])(
      body: (A, B, C) => Unit
  ): Unit =
    val prop =
      Prop.forAll(genA, genB, genC) { (a: A, b: B, c: C) =>
        body(a, b, c)
        true
      }
    checkProperty(prop)

  /**
    * Run a property check with explicit generators for four parameters.
    */
  inline def forAll[A, B, C, D](genA: Gen[A], genB: Gen[B], genC: Gen[C], genD: Gen[D])(
      body: (A, B, C, D) => Unit
  ): Unit =
    val prop =
      Prop.forAll(genA, genB, genC, genD) { (a: A, b: B, c: C, d: D) =>
        body(a, b, c, d)
        true
      }
    checkProperty(prop)

  private inline def checkProperty(prop: Prop): Unit =
    val result = Test.check(testParameters, prop)
    result.status match
      case Test.Passed =>
      // Success
      case Test.Proved(_) =>
      // Success
      case Test.Exhausted =>
        throw AssertionFailure(
          s"Property check exhausted: unable to generate enough test cases after ${result
              .discarded} discarded",
          SourceCode.generate
        )
      case Test.Failed(args, labels) =>
        val argsStr   = args.map(_.arg).mkString(", ")
        val labelsStr =
          if labels.isEmpty then
            ""
          else
            s" [${labels.mkString(", ")}]"
        throw AssertionFailure(
          s"Property check failed for args: (${argsStr})${labelsStr}",
          SourceCode.generate
        )
      case Test.PropException(args, e, labels) =>
        val argsStr   = args.map(_.arg).mkString(", ")
        val labelsStr =
          if labels.isEmpty then
            ""
          else
            s" [${labels.mkString(", ")}]"
        e match
          case af: AssertionFailure =>
            throw af
          case _ =>
            throw AssertionFailure(
              s"Property check failed for args: (${argsStr})${labelsStr}: ${e.getMessage}",
              SourceCode.generate
            )

    end match

  end checkProperty

end PropertyCheck
