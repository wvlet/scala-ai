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

import wvlet.uni.http.{Request, Response}
import wvlet.uni.http.netty.{RxHttpFilter, RxHttpHandler}
import wvlet.uni.http.router.{
  ControllerProvider,
  MapControllerProvider,
  ResponseConverter,
  Route,
  RouteMatcher,
  Router,
  SimpleControllerProvider
}
import wvlet.uni.log.LogSupport
import wvlet.uni.rx.Rx

/**
  * RPC request handler that dispatches requests to controller methods.
  *
  * Handles:
  *   - JSON request body parsing
  *   - Method invocation via Surface reflection
  *   - Response serialization to JSON
  *   - Exception mapping to RPCException responses
  *
  * @param router
  *   The router containing RPC route definitions
  * @param controllerProvider
  *   Provider for obtaining controller instances
  */
class RPCHandler(router: Router, controllerProvider: ControllerProvider)
    extends RxHttpHandler
    with LogSupport:

  private val matcher = RouteMatcher(router.routes)
  private val mapper  = RPCRequestMapper()

  // Lazily initialized filter instance
  private lazy val filterInstance: Option[RxHttpFilter] = router
    .filterSurfaceOpt
    .map { surface =>
      controllerProvider.getFilter(surface) match
        case f: RxHttpFilter =>
          f
        case other =>
          throw ClassCastException(
            s"Expected an RxHttpFilter for ${surface.fullName}, but got ${other.getClass.getName}"
          )
    }

  override def handle(request: Request): Rx[Response] =
    filterInstance match
      case Some(filter) =>
        filter.apply(request, RxHttpHandler(handleRequest))
      case None =>
        handleRequest(request)

  private def handleRequest(request: Request): Rx[Response] =
    matcher.findRoute(request) match
      case Some(routeMatch) =>
        handleRPCRequest(request, routeMatch.route, routeMatch.pathParams)
      case None =>
        debug(s"No RPC route found for ${request.method} ${request.path}")
        Rx.single(
          RPCStatus.NOT_FOUND_U5.newException(s"RPC method not found: ${request.path}").toResponse
        )

  private def handleRPCRequest(
      request: Request,
      route: Route,
      pathParams: Map[String, String]
  ): Rx[Response] =
    try
      val controller = controllerProvider.get(route.controllerSurface)
      val args       = mapper.bindParameters(request, route.methodSurface, Some(controller))
      val result     = route.methodSurface.call(controller, args*)
      ResponseConverter.toResponse(result)
    catch
      case e: RPCException =>
        debug(s"RPC error: ${e.status} - ${e.message}")
        Rx.single(e.toResponse)
      case e: Exception =>
        warn(s"Error handling RPC request: ${e.getMessage}", e)
        Rx.single(RPCStatus.INTERNAL_ERROR_I0.newException(e.getMessage, e).toResponse)

end RPCHandler

object RPCHandler:

  /**
    * Create an RPCHandler with default controller provider.
    */
  def apply(router: Router): RPCHandler = RPCHandler(router, SimpleControllerProvider())

  /**
    * Create an RPCHandler with a custom controller provider.
    */
  def apply(router: Router, controllerProvider: ControllerProvider): RPCHandler =
    new RPCHandler(router, controllerProvider)

  /**
    * Create an RPCHandler with pre-registered controller instances.
    */
  def withControllers(router: Router, controllers: Any*): RPCHandler = RPCHandler(
    router,
    MapControllerProvider(controllers*)
  )

end RPCHandler
