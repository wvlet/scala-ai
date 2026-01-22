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

// Test controllers
class UserController:
  @Endpoint(HttpMethod.GET, "/users")
  def listUsers(): String = "user list"

  @Endpoint(HttpMethod.GET, "/users/:id")
  def getUser(id: String): String = s"user ${id}"

  @Endpoint(HttpMethod.POST, "/users")
  def createUser(): String = "created"

  @Endpoint(HttpMethod.DELETE, "/users/:id")
  def deleteUser(id: String): String = s"deleted ${id}"

class ProductController:
  @Endpoint(HttpMethod.GET, "/products")
  def listProducts(): String = "product list"

  @Endpoint(HttpMethod.GET, "/products/:id")
  def getProduct(id: String): String = s"product ${id}"

class ParamController:
  @Endpoint(HttpMethod.GET, "/search")
  def search(query: String, limit: Int = 10): String = s"query=${query}, limit=${limit}"

  @Endpoint(HttpMethod.GET, "/items/:id/details/:detailId")
  def itemDetail(id: String, detailId: String): String = s"item ${id}, detail ${detailId}"

class RouterTest extends UniTest:

  test("PathComponent.parse should parse path patterns") {
    test("parse simple path") {
      val components = PathComponent.parse("/users")
      components.size shouldBe 1
      components.head shouldMatch { case PathComponent.Literal("users") =>
      }
    }

    test("parse path with parameter") {
      val components = PathComponent.parse("/users/:id")
      components.size shouldBe 2
      components(0) shouldMatch { case PathComponent.Literal("users") =>
      }
      components(1) shouldMatch { case PathComponent.Parameter("id") =>
      }
    }

    test("parse complex path") {
      val components = PathComponent.parse("/api/v1/users/:userId/posts/:postId")
      components.size shouldBe 6
      components(0) shouldMatch { case PathComponent.Literal("api") =>
      }
      components(1) shouldMatch { case PathComponent.Literal("v1") =>
      }
      components(2) shouldMatch { case PathComponent.Literal("users") =>
      }
      components(3) shouldMatch { case PathComponent.Parameter("userId") =>
      }
      components(4) shouldMatch { case PathComponent.Literal("posts") =>
      }
      components(5) shouldMatch { case PathComponent.Parameter("postId") =>
      }
    }

    test("parse empty path") {
      val components = PathComponent.parse("/")
      components.size shouldBe 0
    }
  }

  test("Router.of should extract routes from controller") {
    val router = Router.of[UserController]

    test("extract all endpoints") {
      router.routes.size shouldBe 4
    }

    test("extract GET /users") {
      val route = router.routes.find(r => r.method == HttpMethod.GET && r.pathPattern == "/users")
      (route.isDefined == true) shouldBe true
      route.get.methodSurface.name shouldBe "listUsers"
    }

    test("extract GET /users/:id") {
      val route = router
        .routes
        .find(r => r.method == HttpMethod.GET && r.pathPattern == "/users/:id")
      (route.isDefined == true) shouldBe true
      route.get.methodSurface.name shouldBe "getUser"
    }

    test("extract POST /users") {
      val route = router.routes.find(r => r.method == HttpMethod.POST && r.pathPattern == "/users")
      (route.isDefined == true) shouldBe true
      route.get.methodSurface.name shouldBe "createUser"
    }

    test("extract DELETE /users/:id") {
      val route = router
        .routes
        .find(r => r.method == HttpMethod.DELETE && r.pathPattern == "/users/:id")
      (route.isDefined == true) shouldBe true
      route.get.methodSurface.name shouldBe "deleteUser"
    }
  }

  test("Router.andThen should combine routers") {
    val userRouter    = Router.of[UserController]
    val productRouter = Router.of[ProductController]
    val combined      = userRouter.andThen(productRouter)

    combined.routes.size shouldBe (userRouter.routes.size + productRouter.routes.size)
  }

  test("RouteMatcher should match routes") {
    val router  = Router.of[UserController]
    val matcher = RouteMatcher(router.routes)

    test("match GET /users") {
      val request = Request.get("/users")
      val result  = matcher.findRoute(request)
      (result.isDefined == true) shouldBe true
      result.get.route.methodSurface.name shouldBe "listUsers"
      result.get.pathParams.isEmpty shouldBe true
    }

    test("match GET /users/:id") {
      val request = Request.get("/users/123")
      val result  = matcher.findRoute(request)
      (result.isDefined == true) shouldBe true
      result.get.route.methodSurface.name shouldBe "getUser"
      result.get.pathParams shouldBe Map("id" -> "123")
    }

    test("match POST /users") {
      val request = Request.post("/users")
      val result  = matcher.findRoute(request)
      (result.isDefined == true) shouldBe true
      result.get.route.methodSurface.name shouldBe "createUser"
    }

    test("not match unknown path") {
      val request = Request.get("/unknown")
      val result  = matcher.findRoute(request)
      result.isEmpty shouldBe true
    }

    test("not match wrong method") {
      val request = Request.put("/users")
      val result  = matcher.findRoute(request)
      result.isEmpty shouldBe true
    }
  }

  test("RouteMatcher should extract multiple path params") {
    val router  = Router.of[ParamController]
    val matcher = RouteMatcher(router.routes)

    val request = Request.get("/items/42/details/7")
    val result  = matcher.findRoute(request)
    (result.isDefined == true) shouldBe true
    result.get.pathParams shouldBe Map("id" -> "42", "detailId" -> "7")
  }

  test("HttpRequestMapper should bind parameters") {
    val router  = Router.of[ParamController]
    val matcher = RouteMatcher(router.routes)
    val mapper  = HttpRequestMapper()

    test("bind path parameters") {
      val request    = Request.get("/items/42/details/7")
      val routeMatch = matcher.findRoute(request).get
      val args       = mapper.bindParameters(
        request,
        routeMatch.route.methodSurface,
        routeMatch.pathParams
      )

      args.size shouldBe 2
      args(0) shouldBe "42"
      args(1) shouldBe "7"
    }

    test("bind query parameters") {
      val request = Request
        .get("/search")
        .setQueryParam("query", "test")
        .setQueryParam("limit", "20")
      val routeMatch = matcher.findRoute(request).get
      val args       = mapper.bindParameters(
        request,
        routeMatch.route.methodSurface,
        routeMatch.pathParams
      )

      args.size shouldBe 2
      args(0) shouldBe "test"
      args(1) shouldBe 20
    }

    test("use default values when parameter is missing") {
      val request    = Request.get("/search").setQueryParam("query", "test")
      val routeMatch = matcher.findRoute(request).get
      // Provide controller instance to access method arg default values
      val controller = ParamController()
      val args       = mapper.bindParameters(
        request,
        routeMatch.route.methodSurface,
        routeMatch.pathParams,
        Some(controller)
      )

      args.size shouldBe 2
      args(0) shouldBe "test"
      args(1) shouldBe 10 // default value
    }
  }

  // Note: ResponseConverter tests that use Rx.toSeq are in JVM-specific tests
  // at uni/.jvm/src/test/scala/wvlet/uni/http/router/ResponseConverterJvmTest.scala

  test("ControllerProvider implementations") {
    test("SimpleControllerProvider creates instances") {
      val provider   = SimpleControllerProvider()
      val controller = provider.get(wvlet.uni.surface.Surface.of[UserController])
      (controller != null) shouldBe true
      controller.isInstanceOf[UserController] shouldBe true
    }

    test("MapControllerProvider returns registered instances") {
      val controller = UserController()
      val provider   = MapControllerProvider(controller)
      val retrieved  = provider.get(wvlet.uni.surface.Surface.of[UserController])
      (retrieved == controller) shouldBe true
    }
  }

  test("Router.filter should add filter surface") {
    class TestFilter

    val router = Router.filter[TestFilter].andThen(Router.of[UserController])

    router.filterSurfaceOpt.isDefined shouldBe true
    // In Scala.js, local class names include a suffix (e.g., "TestFilter.1")
    router.filterSurfaceOpt.get.name shouldContain "TestFilter"
  }

end RouterTest
