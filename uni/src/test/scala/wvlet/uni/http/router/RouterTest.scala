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

import wvlet.uni.http.{HttpMethod, Request, Response}
import wvlet.uni.test.UniTest

// Single test controller to minimize Surface memory usage
class TestController:
  @Endpoint(HttpMethod.GET, "/users")
  def listUsers(): String = "user list"

  @Endpoint(HttpMethod.GET, "/users/:id")
  def getUser(id: String): String = s"user ${id}"

  @Endpoint(HttpMethod.GET, "/search")
  def search(query: String, limit: Int = 10): String = s"query=${query}, limit=${limit}"

class RouterTest extends UniTest:
  // Single shared router instance
  private lazy val router = Router.of[TestController]

  test("PathComponent should parse paths") {
    // Simple path
    PathComponent.parse("/users").size shouldBe 1

    // Path with parameter
    val withParam = PathComponent.parse("/users/:id")
    withParam.size shouldBe 2
    withParam(1) shouldMatch { case PathComponent.Parameter("id") =>
    }

    // Empty path
    PathComponent.parse("/").size shouldBe 0
  }

  test("Router.of should extract routes") {
    router.routes.size shouldBe 3

    // Verify routes are extracted correctly
    val getUsers = router.routes.find(_.pathPattern == "/users")
    getUsers.isDefined shouldBe true
    getUsers.get.method shouldBe HttpMethod.GET
  }

  test("RouteMatcher should match and extract params") {
    val matcher = RouteMatcher(router.routes)

    // Match exact path
    val usersMatch = matcher.findRoute(Request.get("/users"))
    usersMatch.isDefined shouldBe true
    usersMatch.get.pathParams.isEmpty shouldBe true

    // Match with path param
    val userMatch = matcher.findRoute(Request.get("/users/123"))
    userMatch.isDefined shouldBe true
    userMatch.get.pathParams shouldBe Map("id" -> "123")

    // No match for unknown path
    matcher.findRoute(Request.get("/unknown")).isEmpty shouldBe true
  }

  test("HttpRequestMapper should bind parameters") {
    val matcher = RouteMatcher(router.routes)
    val mapper  = HttpRequestMapper()

    // Bind query params
    val request = Request.get("/search").setQueryParam("query", "test").setQueryParam("limit", "20")
    val routeMatch = matcher.findRoute(request).get
    val args = mapper.bindParameters(request, routeMatch.route.methodSurface, routeMatch.pathParams)

    args.size shouldBe 2
    args(0) shouldBe "test"
    args(1) shouldBe 20
  }

  test("ControllerProvider should provide instances") {
    val provider   = SimpleControllerProvider()
    val controller = provider.get(router.routes.head.controllerSurface)
    controller.isInstanceOf[TestController] shouldBe true
  }

  test("Router.filter should add filter") {
    class TestFilter
    val filtered = Router.filter[TestFilter].andThen(router)
    filtered.filterSurfaceOpt.isDefined shouldBe true
  }

end RouterTest
