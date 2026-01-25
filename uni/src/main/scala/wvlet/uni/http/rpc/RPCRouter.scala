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
package wvlet.uni.http.rpc

import wvlet.uni.http.HttpMethod
import wvlet.uni.http.router.{PathComponent, Route, Router}
import wvlet.uni.surface.{MethodSurface, Surface}

import scala.quoted.*

/**
  * RPC Router builder.
  *
  * Builds a Router from any trait where all public methods become POST endpoints at:
  * `/rpc/{ServiceName}/{methodName}`
  *
  * Example:
  * {{{
  * trait UserService:
  *   def getUser(id: Long): User
  *   def createUser(name: String): User
  *
  * val router = RPCRouter.of[UserService]
  * // Creates routes:
  * //   POST /rpc/UserService/getUser
  * //   POST /rpc/UserService/createUser
  * }}}
  */
object RPCRouter:

  /**
    * Build a Router from an RPC trait with default prefix "/rpc"
    */
  inline def of[T]: Router =
    ${
      buildRPCRouterImpl[T](
        '{
          "/rpc"
        }
      )
    }

  /**
    * Build a Router from an RPC trait with custom prefix
    */
  inline def of[T](prefix: String): Router =
    ${
      buildRPCRouterImpl[T]('prefix)
    }

  private def buildRPCRouterImpl[T: Type](prefix: Expr[String])(using Quotes): Expr[Router] =
    import quotes.reflect.*

    '{
      val surface        = Surface.of[T]
      val methodSurfaces = Surface.methodsOf[T]
      val routes         = RPCRouter.extractRPCRoutes(
        ${
          prefix
        },
        surface,
        methodSurfaces
      )
      Router(routes, None)
    }

  /**
    * Extract RPC routes from method surfaces. Each public method becomes a POST endpoint.
    */
  def extractRPCRoutes(
      prefix: String,
      controllerSurface: Surface,
      methodSurfaces: Seq[MethodSurface]
  ): Seq[Route] =
    val serviceName = controllerSurface.name
    methodSurfaces
      .filter(_.isPublic)
      .map { ms =>
        val path           = s"${prefix}/${serviceName}/${ms.name}"
        val pathComponents = PathComponent.parse(path)
        Route(HttpMethod.POST, path, pathComponents, controllerSurface, ms)
      }

end RPCRouter
