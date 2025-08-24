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

import java.math.BigInteger
import scala.concurrent.Future
import scala.util.Try

object Examples:
  case class A(b: Boolean, bt: Byte, st: Short, i: Int, l: Long, f: Float, d: Double, str: String)

  case class B(a: A)

  type MyA = A

  trait C

  type MyInt = Int
  type MyMap = Map[Int, String]

  case class D[V](id: Int, v: V)

  trait Service[-Req, +Rep] extends (Req => Future[Rep])

  case class E(a: A)
  case class F(p0: Int = 10)

import Examples.*

/**
  */
class SurfaceTest extends SurfaceSpec:
  test("resolve types") {
    val a = check(TypeShape.of[A], "A")
    assert(a.isAlias == false)
    assert(a.isOption == false)
    assert(a.isPrimitive == false)

    val b = check(TypeShape.of[B], "B")
    assert(b.isAlias == false)
    assert(b.isOption == false)
    assert(b.isPrimitive == false)
  }

  test("resolve primitive types") {
    checkPrimitive(TypeShape.of[Boolean], "Boolean")
    checkPrimitive(TypeShape.of[Byte], "Byte")
    checkPrimitive(TypeShape.of[Short], "Short")
    checkPrimitive(TypeShape.of[Int], "Int")
    checkPrimitive(TypeShape.of[Long], "Long")
    checkPrimitive(TypeShape.of[Float], "Float")
    checkPrimitive(TypeShape.of[Double], "Double")
    checkPrimitive(TypeShape.of[String], "String")
    checkPrimitive(TypeShape.of[Char], "Char")
    checkPrimitive(TypeShape.of[java.lang.String], "String")
  }

  test("find primitive Surfaces") {
    Primitive(classOf[Int]) shouldBe Primitive.Int
  }

  test("be equal") {
    val a1 = TypeShape.of[A]
    val a2 = TypeShape.of[A]

    // In Scala 3, TypeShape instance identity is not guaranteed
    // assert(a1 eq a2)

    // equality
    assert(a1 == a2)
    assert(a1.hashCode() == a2.hashCode())

    val b  = TypeShape.of[B]
    val a3 = b.params.head.typeShape

    // assert(a1 eq a3)

    // Generic surface
    val c1 = TypeShape.of[Seq[Int]]
    val c2 = TypeShape.of[Seq[Int]]
    assert(c1.equals(c2) == true)
    // assert(c1 eq c2)
    assert(c1.hashCode() == c2.hashCode())

    assert(c1 ne a1)
    assert(c1.equals(a1) == false)
    assert(c1.equals("hello") == false)
  }

  test("resolve alias") {
    val a1 = check(TypeShape.of[MyA], "MyA:=A")
    assert(a1.isAlias == true)
    assert(a1.isOption == false)

    val a2 = check(TypeShape.of[MyInt], "MyInt:=Int")
    assert(a2.isAlias == true)
    assert(a1.isOption == false)

    val a3 = check(TypeShape.of[MyMap], "MyMap:=Map[Int,String]")
    assert(a3.isAlias == true)
    assert(a1.isOption == false)
  }

  test("resolve trait") {
    check(TypeShape.of[C], "C")
  }

  test("resolve array types") {
    check(TypeShape.of[Array[Int]], "Array[Int]")
    check(TypeShape.of[Array[Byte]], "Array[Byte]")
    check(TypeShape.of[Array[A]], "Array[A]")
  }

  test("resolve option types") {
    val opt = check(TypeShape.of[Option[A]], "Option[A]")
    assert(opt.isOption == true)
  }

  test("resolve collection types") {
    check(TypeShape.of[Seq[A]], "Seq[A]")
    check(TypeShape.of[List[A]], "List[A]")
    check(TypeShape.of[Map[String, A]], "Map[String,A]")
    check(TypeShape.of[Map[String, Long]], "Map[String,Long]")
    check(TypeShape.of[Map[Long, B]], "Map[Long,B]")
    check(TypeShape.of[Set[String]], "Set[String]")
    check(TypeShape.of[IndexedSeq[A]], "IndexedSeq[A]")
  }

  test("resolve scala util types") {
    check(TypeShape.of[Either[String, Throwable]], "Either[String,Throwable]")
    check(TypeShape.of[Try[A]], "Try[A]")
  }

  test("resolve mutable Collection types") {
    check(TypeShape.of[collection.mutable.Seq[String]], "Seq[String]")
    check(TypeShape.of[collection.mutable.Map[Int, String]], "Map[Int,String]")
    check(TypeShape.of[collection.mutable.Set[A]], "Set[A]")
  }

  test("resolve tuples") {
    check(TypeShape.of[Tuple1[Int]], "Tuple1[Int]")
    check(TypeShape.of[(Int, String)], "Tuple2[Int,String]")
    check(TypeShape.of[(Int, String, A, Double)], "Tuple4[Int,String,A,Double]")
  }

  test("resolve java colletion type") {
    check(TypeShape.of[java.util.List[String]], "List[String]")
    check(TypeShape.of[java.util.Map[Long, String]], "Map[Long,String]")
    check(TypeShape.of[java.util.Set[A]], "Set[A]")
  }

  test("resolve generic type") {
    val d1 = check(TypeShape.of[D[String]], "D[String]")
    val d2 = check(TypeShape.of[D[A]], "D[A]")
    assert(d1 ne d2, "should not be the same instance")
  }

  test("resolve recursive type") {
    check(TypeShape.of[Service[Int, String]], "Service[Int,String]")
  }

  test("resolve generic abstract type") {
    TypeShape.of[D[?]].typeArgs shouldBe Seq(ExistentialType)
    val d = check(TypeShape.of[D[?]], "D[?]")
    d.typeArgs.length shouldBe 1
    check(TypeShape.of[Map[?, ?]], "Map[?,?]")
  }

  val a0 = A(true, 0.toByte, 1.toShort, 10, 20L, 0.1f, 0.2, "hello")

  test("generate object factory") {
    val a = check(TypeShape.of[A], "A")
    assert(a.objectFactory.isDefined)

    val a1 = a
      .objectFactory
      .map(_.newInstance(Seq(true, 0.toByte, 1.toShort, 10, 20L, 0.1f, 0.2, "hello")))
    debug(a1)
    assert(a1.get == a0)

    val e = check(TypeShape.of[E], "E")
    assert(e.objectFactory.isDefined)
    val e1: E = e.objectFactory.map(_.newInstance(Seq(a0))).get.asInstanceOf[E]
    debug(e1)
    assert(e1.a == a0)
  }

  test("generate concrete object factory") {
    val d = check(TypeShape.of[D[String]], "D[String]")
    val d0 =
      d.objectFactory
        .map { f =>
          f.newInstance(Seq(1, "leo"))
        }
        .get
    debug(d0)
    assert(d0 == D(1, "leo"))
  }

  test("find default parameter") {
    val f = check(TypeShape.of[F], "F")
    val p = f.params(0)
    assert(p.getDefaultValue.isDefined)
    assert(p.getDefaultValue.get == 10)
  }

  test("access parameters") {
    val a = TypeShape.of[A]
    assert(a.params(0).get(a0) == true)
    assert(a.params(3).get(a0) == 10)
    assert(a.params(4).get(a0) == 20L)
    assert(a.params(7).get(a0) == "hello")
  }

  test("object factory") {
    val s = TypeShape.of[F]
    assert(s.objectFactory.isDefined)
    val f = s.objectFactory.map(_.newInstance(Seq(100)))
    f shouldBe Some(F(100))
  }

  test("bigint") {
    TypeShape.of[BigInt]
    TypeShape.of[BigInteger]
  }

  test("resolve types args of Map[String, Any]") {
    val s = TypeShape.of[Map[String, Any]]
    s.typeArgs(0).fullName shouldBe "java.lang.String"
    s.typeArgs(1).fullName shouldBe "scala.Any"
  }

end SurfaceTest
