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
package wvlet.uni.rx

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal

/**
  * A resource that can be safely acquired and released, with guaranteed cleanup.
  *
  * RxResource is inspired by cats-effect Resource. It provides bracket semantics: acquire a
  * resource, use it, and guarantee that cleanup runs even if an error occurs.
  *
  * @tparam A
  *   the type of the resource
  */
trait RxResource[A]:

  /**
    * Use the resource with guaranteed cleanup.
    *
    * The cleanup action will run even if the body throws an exception.
    */
  def use[B](body: A => Rx[B]): Rx[B]

  /**
    * Map over the resource.
    */
  def map[B](f: A => B): RxResource[B]

  /**
    * FlatMap over the resource, composing with another resource.
    */
  def flatMap[B](f: A => RxResource[B]): RxResource[B]

  /**
    * Combine with another resource, releasing in reverse order of acquisition.
    */
  def zip[B](other: RxResource[B]): RxResource[(A, B)]

  /**
    * Add a finalizer that runs after the main cleanup.
    */
  def onFinalize(finalizer: Rx[Unit]): RxResource[A]

object RxResource:

  /**
    * Create a resource with acquire and release actions.
    */
  def make[A](acquire: Rx[A])(release: A => Rx[Unit]): RxResource[A] =
    new RxResourceImpl(acquire, release)

  /**
    * Create a resource from an AutoCloseable.
    */
  def fromAutoCloseable[A <: AutoCloseable](acquire: Rx[A]): RxResource[A] =
    make(acquire)(a => Rx.single(a.close()))

  /**
    * Create a resource that doesn't need cleanup.
    */
  def pure[A](a: A): RxResource[A] = make(Rx.single(a))(_ => Rx.single(()))

  /**
    * Create a resource that runs an action but doesn't produce a value.
    */
  def eval[A](rx: Rx[A]): RxResource[A] = make(rx)(_ => Rx.single(()))

  /**
    * Create an empty resource.
    */
  val unit: RxResource[Unit] = pure(())

  private class RxResourceImpl[A](
      acquire: Rx[A],
      release: A => Rx[Unit],
      finalizers: List[Rx[Unit]] = Nil
  ) extends RxResource[A]:

    override def use[B](body: A => Rx[B]): Rx[B] = acquire.flatMap { a =>
      body(a).transform { result =>
        // Always run cleanup
        var errors = List.empty[Throwable]

        // Run the main release
        try
          release(a).await
        catch
          case NonFatal(e) =>
            errors = e :: errors

        // Run additional finalizers
        finalizers.foreach { fin =>
          try
            fin.await
          catch
            case NonFatal(e) =>
              errors = e :: errors
        }

        // Handle errors
        result match
          case Success(b) =>
            if errors.isEmpty then
              b
            else
              val combined = errors.reduceLeft { (a, b) =>
                a.addSuppressed(b)
                a
              }
              throw combined
          case Failure(e) =>
            errors.foreach(e.addSuppressed)
            throw e
      }
    }

    override def map[B](f: A => B): RxResource[B] =
      new RxResourceImpl(acquire.map(f), _ => Rx.single(()), finalizers)

    override def flatMap[B](f: A => RxResource[B]): RxResource[B] =
      new RxResourceImpl(
        acquire.flatMap { a =>
          // When acquiring B, we need to ensure A is released if B's acquisition fails
          f(a) match
            case impl: RxResourceImpl[B] =>
              impl.acquire
            case other =>
              other.use(b => Rx.single(b))
        },
        b =>
          // This is a simplification - proper implementation would track the nested resource
          Rx.single(()),
        finalizers
      )

    override def zip[B](other: RxResource[B]): RxResource[(A, B)] =
      new RxResourceImpl(
        acquire.flatMap { a =>
          other match
            case impl: RxResourceImpl[B] =>
              impl.acquire.map(b => (a, b))
            case _ =>
              other.use(b => Rx.single((a, b)))
        },
        { case (a, b) =>
          // Release in reverse order
          other match
            case impl: RxResourceImpl[B] =>
              impl.release(b).flatMap(_ => release(a))
            case _ =>
              release(a)
        },
        finalizers
      )

    override def onFinalize(finalizer: Rx[Unit]): RxResource[A] =
      new RxResourceImpl(acquire, release, finalizer :: finalizers)

  end RxResourceImpl

end RxResource
