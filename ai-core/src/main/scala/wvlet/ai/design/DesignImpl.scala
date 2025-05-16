package wvlet.ai.design

import wvlet.ai.surface.Surface
import wvlet.ai.log.LogSupport
import wvlet.ai.util.SourceCode

/**
  * Immutable airframe design.
  *
  * Design instance does not hold any duplicate bindings for the same Surface.
  */
private[design] trait DesignImpl extends LogSupport:
  self: Design =>
  private inline def bind[A](using sourceCode: SourceCode): Binder[A] =
    new Binder(self, Surface.of[A], sourceCode).asInstanceOf[Binder[A]]

  inline def remove[A]: Design =
    val target = Surface.of[A]
    new Design(self.designOptions, self.binding.filterNot(_.from == target), self.hooks)

  inline def bindInstance[A](obj: A)(using sourceCode: SourceCode): DesignWithContext[A] = bind[A]
    .toInstance(obj)

  inline def bindSingleton[A](using sourceCode: SourceCode): DesignWithContext[A] =
    bind[A].toSingleton

  inline def bindEagerSingleton[A](using sourceCode: SourceCode): DesignWithContext[A] =
    bind[A].toEagerSingleton

  inline def bindImpl[A, B <: A](using sourceCode: SourceCode): DesignWithContext[B] = bind[A].to[B]

  inline def bindProvider[D1, A](f: D1 => A)(using sourceCode: SourceCode): DesignWithContext[A] =
    bind[A].toProvider[D1](f)

  inline def bindProvider[D1, D2, A](f: (D1, D2) => A)(using
      sourceCode: SourceCode
  ): DesignWithContext[A] = bind[A].toProvider[D1, D2](f)

  inline def bindProvider[D1, D2, D3, A](f: (D1, D2, D3) => A): DesignWithContext[A] = bind[A]
    .toProvider[D1, D2, D3](f)

  inline def bindProvider[D1, D2, D3, D4, A](f: (D1, D2, D3, D4) => A): DesignWithContext[A] =
    bind[A].toProvider[D1, D2, D3, D4](f)

  inline def bindProvider[D1, D2, D3, D4, D5, A](
      f: (D1, D2, D3, D4, D5) => A
  ): DesignWithContext[A] = bind[A].toProvider[D1, D2, D3, D4, D5](f)

  /**
    * A helper method of creating a new session and an instance of A. This method is useful when you
    * only need to use A as an entry point of your program. After executing the body, the sesion
    * will be closed.
    *
    * @param body
    * @tparam A
    * @return
    */
  inline def build[A](body: A => Any): Any = self.withSession { session =>
    val a = session.build[A]
    body(a)
  }

  /**
    * Execute a given code block by building A using this design, and return B
    */
  inline def run[A, B](body: A => B): B = {
    self.withSession { session =>
      val a = session.build[A]
      body(a)
    }
  }.asInstanceOf[B]

end DesignImpl
