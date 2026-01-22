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
package wvlet.uni.http

/**
  * REST Router framework for defining HTTP endpoints using annotations.
  *
  * Example usage:
  * {{{
  * import wvlet.uni.http.router.*
  * import wvlet.uni.http.{HttpMethod, Request, Response}
  *
  * class UserController:
  *   @Endpoint(HttpMethod.GET, "/users")
  *   def listUsers(): Seq[User] = ???
  *
  *   @Endpoint(HttpMethod.GET, "/users/:id")
  *   def getUser(id: String): User = ???
  *
  *   @Endpoint(HttpMethod.POST, "/users")
  *   def createUser(request: Request): User = ???
  *
  * // Build router
  * val router = Router.of[UserController]
  *
  * // With filter
  * val routerWithFilter = Router
  *   .filter[LogFilter]
  *   .andThen(Router.of[UserController])
  * }}}
  */
package object router
