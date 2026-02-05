# uni-dom Router Implementation Plan

## Overview

Add client-side routing to uni-dom using the History API. This is the most impactful missing feature for building Single Page Applications (SPAs).

## API Design

### Core Types

```scala
// URL location data
case class Location(
    pathname: String,
    search: String,
    hash: String
)

// Parsed route parameters
case class RouteParams(
    path: Map[String, String] = Map.empty,   // :id -> value
    query: Map[String, String] = Map.empty,  // ?key=value
    hash: Option[String] = None              // #section
)

// Route definition
case class Route[A](
    pattern: String,                         // "/users/:id"
    render: RouteParams => A                 // params => UserPage(params)
)
```

### Router Object (Global Navigation)

```scala
object Router:
  // Reactive location state
  def location: Rx[Location]
  def pathname: Rx[String]
  def search: Rx[String]
  def hash: Rx[String]

  // Programmatic navigation
  def push(path: String): Unit      // history.pushState
  def replace(path: String): Unit   // history.replaceState
  def back(): Unit                  // history.back
  def forward(): Unit               // history.forward

  // Create router instance
  def apply[A](routes: Route[A]*): RouterInstance[A]
```

### RouterInstance (Route Matching)

```scala
class RouterInstance[A](routes: Seq[Route[A]]):
  // Current matched route
  def outlet: Rx[A]                 // Throws if no route matches
  def outletOption: Rx[Option[A]]   // Returns None if no route matches
  def params: Rx[RouteParams]

  // Navigation helpers
  def link(path: String, children: DomNode*): RxElement
  def isActive(path: String): Rx[Boolean]
  def isActiveExact(path: String): Rx[Boolean]
```

## Usage Examples

```scala
import wvlet.uni.dom.all.*

// Define routes
val router = Router(
  Route("/", _ => div("Home Page")),
  Route("/users", _ => div("User List")),
  Route("/users/:id", p => div(s"User ${p.path("id")}")),
  Route("/posts/:postId/comments/:commentId", p =>
    div(s"Post ${p.path("postId")} Comment ${p.path("commentId")}")
  ),
  Route("*", _ => div("404 Not Found"))
)

// Main app
def App() = div(
  nav(
    router.link("/", "Home"),
    router.link("/users", "Users"),
    router.link("/users/123", "User 123")
  ),
  main(
    router.outletOption.map(_.getOrElse(div("Loading...")))
  )
)

// Programmatic navigation
button(onclick -> { () => Router.push("/users/456") }, "Go to User 456")
button(onclick -> { () => Router.back() }, "Back")
```

## Implementation Details

### Route Pattern Matching

- `/users` - Exact match
- `/users/:id` - Named parameter (captures "id")
- `/users/:id/posts/:postId` - Multiple parameters
- `*` - Wildcard (catch-all, matches any path)

Pattern parsing algorithm:
1. Split pattern by `/`
2. For each segment:
   - If starts with `:`, it's a parameter
   - Otherwise, literal match
3. Extract parameter values from matching URL segments

### History API Integration

```scala
// Listen for popstate (back/forward buttons)
dom.window.addEventListener("popstate", handler)

// Update URL without page reload
dom.window.history.pushState(null, "", path)
dom.window.history.replaceState(null, "", path)
```

### Link Component

Creates `<a>` elements that:
- Prevent default navigation
- Call `Router.push()` instead
- Support active state styling

```scala
def link(path: String, children: DomNode*): DomElement =
  a(
    href -> path,
    onclick -> { (e: dom.MouseEvent) =>
      e.preventDefault()
      Router.push(path)
    },
    cls.toggle(isActive(path), "active"),
    children*
  )
```

## Files to Create

### New Files
| File | Description |
|------|-------------|
| `uni/.js/src/main/scala/wvlet/uni/dom/Router.scala` | Core router implementation |
| `uni-dom-test/src/test/scala/wvlet/uni/dom/RouterTest.scala` | Unit tests |

### Files to Modify
| File | Change |
|------|--------|
| `uni/.js/src/main/scala/wvlet/uni/dom/all.scala` | Export Router, Route, RouteParams, Location |

## Verification

```bash
# Compile
./sbt "uniJS/compile"

# Run tests
./sbt "uniDomTest/testOnly *RouterTest"

# Format
./sbt scalafmtAll
```

## Future Enhancements (Not in this PR)

- Nested routes
- Route guards (beforeEnter hooks)
- Lazy route loading
- Query string builder utilities
- Hash-based routing mode (for static hosting)
