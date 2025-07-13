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

package wvlet.ai.core.typeshape

import tag.@@

import java.math.BigInteger

/**
  */
class ZeroTest extends SurfaceSpec:
  import ZeroTest.*

  private def zeroCheck[P](typeShape: TypeShape, v: P): P =
    val z = Zero.zeroOf(typeShape).asInstanceOf[P]
    typeShape match
      case s: ArrayTypeShape =>
        pendingUntil("array comparison")
      case _ =>
        z shouldBe v
    z

  test("support primitives") {
    zeroCheck(TypeShape.of[Unit], null)
    zeroCheck(TypeShape.of[Int], 0)
    zeroCheck(TypeShape.of[Long], 0L)
    zeroCheck(TypeShape.of[Char], 0.toChar)
    zeroCheck(TypeShape.of[Boolean], false)
    zeroCheck(TypeShape.of[Short], 0.toShort)
    zeroCheck(TypeShape.of[Byte], 0.toByte)
    zeroCheck(TypeShape.of[Float], 0f)
    zeroCheck(TypeShape.of[Double], 0.0)
    zeroCheck(TypeShape.of[String], "")
  }

  test("bigint") {
    zeroCheck(TypeShape.of[BigInt], BigInt(0))
    zeroCheck(TypeShape.of[BigInteger], BigInteger.ZERO)
  }

  test("support arrays") {
    zeroCheck(TypeShape.of[Array[Int]], Array.empty[Int])
    zeroCheck(TypeShape.of[Array[Long]], Array.empty[Long])
    zeroCheck(TypeShape.of[Array[String]], Array.empty[String])
  }

  test("support Tuple") {
    zeroCheck(TypeShape.of[(Int, String)], (0, ""))
    zeroCheck(TypeShape.of[(Int, String, Seq[Int])], (0, "", Seq.empty))
  }

  test("special types") {
    zeroCheck(TypeShape.of[MyA], "")
    zeroCheck(TypeShape.of[Int @@ MyTag], 0)
    zeroCheck(TypeShape.of[Nothing], null)
    zeroCheck(TypeShape.of[AnyRef], null)
    zeroCheck(TypeShape.of[Any], null)
    zeroCheck(TypeShape.of[Option[String]], None)
    true
  }

  test("support case classes") {
    val s = TypeShape.of[ZeroA]
    zeroCheck(TypeShape.of[ZeroA], ZeroA(0, "", ZeroB(0.0f, 0.0)))
    // Read the default parameter values.
    // Disabled the check because Scala.js doesn't support reading the default values:
    // https://github.com/wvlet/airframe/issues/149
    // zeroCheck(TypeShape.of[C], C(10, "Hello", 123.4f, B(0.0f, 0.0)))
  }

  test("support Scala collections") {
    zeroCheck(TypeShape.of[Seq[Int]], Seq.empty[Int])
    zeroCheck(TypeShape.of[IndexedSeq[Int]], IndexedSeq.empty[Int])
    zeroCheck(TypeShape.of[Map[Int, String]], Map.empty[Int, String])
    zeroCheck(TypeShape.of[Set[Int]], Set.empty[Int])
    zeroCheck(TypeShape.of[List[Int]], List.empty[Int])
    true
  }

end ZeroTest

object ZeroTest:
  trait MyTag
  type MyA = String

  case class ZeroA(i: Int, s: String, b: ZeroB)
  case class ZeroB(f: Float, d: Double)
  // case class C(i: Int = 10, s: String = "Hello", f: Float = 123.4f, b: B)
