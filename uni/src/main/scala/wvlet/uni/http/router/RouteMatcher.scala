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
package wvlet.uni.http.router

import wvlet.uni.http.{HttpMethod, Request}

/**
  * Matches incoming HTTP requests to routes.
  *
  * @param routes
  *   The routes to match against
  */
class RouteMatcher(routes: Seq[Route]):

  /**
    * Find a matching route for the given request.
    *
    * @param request
    *   The incoming HTTP request
    * @return
    *   Some(RouteMatch) if a matching route is found, None otherwise
    */
  def findRoute(request: Request): Option[RouteMatch] =
    val requestPath       = request.path
    val requestMethod     = request.method
    val requestComponents = parsePathSegments(requestPath)

    routes
      .find(route => matches(route, requestMethod, requestComponents))
      .map { route =>
        val pathParams = extractParams(route, requestComponents)
        RouteMatch(route, pathParams)
      }

  /**
    * Check if a route matches the request method and path components.
    */
  private def matches(
      route: Route,
      requestMethod: HttpMethod,
      requestComponents: Seq[String]
  ): Boolean =
    if route.method != requestMethod then
      false
    else if route.pathComponents.size != requestComponents.size then
      false
    else
      route
        .pathComponents
        .zip(requestComponents)
        .forall {
          case (PathComponent.Literal(expected), actual) =>
            expected == actual
          case (PathComponent.Parameter(_), _) =>
            // Parameters match any value
            true
        }

  /**
    * Extract path parameter values from a matching route.
    */
  private def extractParams(route: Route, requestComponents: Seq[String]): Map[String, String] =
    route
      .pathComponents
      .zip(requestComponents)
      .collect { case (PathComponent.Parameter(name), value) =>
        name -> value
      }
      .toMap

  /**
    * Parse a request path into segments.
    */
  private def parsePathSegments(path: String): Seq[String] =
    val normalizedPath =
      if path.startsWith("/") then
        path.substring(1)
      else
        path
    if normalizedPath.isEmpty then
      Seq.empty
    else
      normalizedPath.split("/").toSeq

end RouteMatcher

/**
  * Represents a successful route match with extracted path parameters.
  *
  * @param route
  *   The matched route
  * @param pathParams
  *   Map of path parameter names to their extracted values
  */
case class RouteMatch(route: Route, pathParams: Map[String, String])
