package wvlet.ai.core

object ChainingUtil:
  implicit class ChainOps[A](self: A) extends AnyVal:
    def pipe[B](f: A => B): B = f(self)
    def ifDefined[B](opt: Option[B])(thenFn: (A, B) => A): A =
      opt match
        case Some(v) =>
          thenFn(self, v)
        case None =>
          self
