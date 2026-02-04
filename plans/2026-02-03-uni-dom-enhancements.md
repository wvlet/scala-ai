# uni-dom Enhancement Plan: Browser API Integration

## Overview

uni-dom already provides comprehensive HTML/SVG elements, attributes, events, CSS properties, ARIA support, and reactive two-way binding. This plan adds four practical browser API integrations to make it a more complete DOM library.

## Current State

**Already Implemented:**
- 60+ HTML elements, 60+ SVG elements with proper namespace handling
- Comprehensive attributes (global, input, link, form, table, script, meta)
- 50+ event handlers (mouse, keyboard, form, focus, drag, touch, pointer, animation, media)
- 80+ CSS properties with grouped CSS syntax: `style(display := "flex", gap := "8px")`
- 30+ ARIA attributes
- Two-way binding: `value.bind(rxVar)`, `checked.bind(rxVar)`
- Reactive integration with Rx library
- Lifecycle hooks: `beforeRender`, `onMount`, `beforeUnmount`

## Features to Add

### 1. IntersectionObserver Integration
Reactive visibility detection for lazy loading and infinite scroll.

```scala
val isVisible = Rx.variable(false)
div(
  Intersection.observe(isVisible),
  isVisible.map { visible =>
    if visible then img(src -> "/heavy-image.jpg")
    else div(cls -> "placeholder")
  }
)
```

### 2. ResizeObserver Integration
Reactive element size tracking for responsive components.

```scala
val size = Rx.variable((0.0, 0.0))
div(
  Resize.observe(size),
  size.map { case (w, _) =>
    if w > 768 then "desktop-layout" else "mobile-layout"
  }
)
```

### 3. LocalStorage Reactive Binding
Persistent state with automatic serialization and cross-tab sync.

```scala
val theme = Storage.local("theme", "light")
html(
  cls -> theme,
  button(onclick -> { () => theme := "dark" }, "Dark Mode")
)
```

### 4. Portal/Teleport
Render children outside parent DOM hierarchy (modals, tooltips, dropdowns).

```scala
div(
  style := "overflow: hidden",
  Portal.toBody(
    div(cls -> "modal", "Modal escapes overflow:hidden")
  )
)
```

## Implementation

### New Files

| File | Description |
|------|-------------|
| `uni/.js/src/main/scala/wvlet/uni/dom/DomObservers.scala` | IntersectionObserver + ResizeObserver |
| `uni/.js/src/main/scala/wvlet/uni/dom/Storage.scala` | LocalStorage/SessionStorage bindings |
| `uni/.js/src/main/scala/wvlet/uni/dom/Portal.scala` | Portal/Teleport functionality |
| `uni-dom-test/src/test/scala/wvlet/uni/dom/DomObserversTest.scala` | Observer tests |
| `uni-dom-test/src/test/scala/wvlet/uni/dom/StorageTest.scala` | Storage tests |
| `uni-dom-test/src/test/scala/wvlet/uni/dom/PortalTest.scala` | Portal tests |

### Modified Files

| File | Changes |
|------|---------|
| `uni/.js/src/main/scala/wvlet/uni/dom/DomRenderer.scala` | Handle new DomNode types in `traverse` |
| `uni/.js/src/main/scala/wvlet/uni/dom/all.scala` | Export new APIs |

## API Design

### IntersectionObserver

```scala
object Intersection:
  // Simple visibility tracking
  def observe(target: RxVar[Boolean], config: IntersectionConfig = IntersectionConfig.default): DomNode

  // Full entry with ratio, bounds, etc.
  def observeEntry(target: RxVar[Option[IntersectionEntry]], config: IntersectionConfig = IntersectionConfig.default): DomNode

  // One-time callback when visible (for lazy loading)
  def onceVisible(callback: () => Unit, config: IntersectionConfig = IntersectionConfig.default): DomNode

case class IntersectionConfig(
    root: Option[dom.Element] = None,
    rootMargin: String = "0px",
    threshold: Seq[Double] = Seq(0.0)
)
```

### ResizeObserver

```scala
object Resize:
  // Track (width, height)
  def observe(target: RxVar[(Double, Double)]): DomNode

  // Full entry with border/content box
  def observeEntry(target: RxVar[Option[ResizeEntry]]): DomNode

  // Debounced for performance
  def observeDebounced(target: RxVar[(Double, Double)], debounceMs: Int = 100): DomNode
```

### Storage

```scala
object Storage:
  // LocalStorage-backed RxVar
  def local[A: StorageCodec](key: String, default: A): RxVar[A]

  // SessionStorage-backed RxVar
  def session[A: StorageCodec](key: String, default: A): RxVar[A]

  def removeLocal(key: String): Unit
  def removeSession(key: String): Unit

trait StorageCodec[A]:
  def encode(value: A): String
  def decode(raw: String): Option[A]

// Built-in codecs: String, Int, Long, Double, Boolean
```

### Portal

```scala
object Portal:
  // Render to element by ID (creates if missing)
  def to(targetId: String)(children: DomNode*): DomNode

  // Render to document.body
  def toBody(children: DomNode*): DomNode

  // Render to specific element
  def toElement(target: dom.Element)(children: DomNode*): DomNode
```

## Implementation Pattern

Follow existing binding pattern in `DomProperty.scala` and `DomRenderer.scala`:

1. Define case class extending `DomNode`:
   ```scala
   case class IntersectionBinding(target: RxVar[Boolean], config: IntersectionConfig) extends DomNode
   ```

2. Handle in `DomRenderer.traverse`:
   ```scala
   case ib: IntersectionBinding =>
     handleIntersectionBinding(node, ib)
   ```

3. Implement handler returning `Cancelable` for cleanup:
   ```scala
   private def handleIntersectionBinding(node: dom.Node, binding: IntersectionBinding): Cancelable =
     val observer = IntersectionObserver(...)
     observer.observe(node.asInstanceOf[dom.Element])
     Cancelable(() => observer.disconnect())
   ```

## Verification

```bash
# Compile
./sbt compile

# Run tests
./sbt "uniJS/testOnly *DomObserversTest"
./sbt "uniJS/testOnly *StorageTest"
./sbt "uniJS/testOnly *PortalTest"

# Format
./sbt scalafmtAll
```

## Implementation Sequence

1. **Storage** - Most self-contained, extends RxVar pattern
2. **Portal** - Straightforward DOM manipulation
3. **IntersectionObserver** - Browser API binding
4. **ResizeObserver** - Similar pattern to IntersectionObserver
5. Update `all.scala` exports
6. Add tests for all features

## Implementation Notes

### Learnings from PR Review

1. **StorageVar lifecycle**: Override `stop()` to dispose the storage listener when the RxVar is cancelled, ensuring proper cleanup when components unmount.

2. **JS facade type accuracy**: Browser APIs like `IntersectionObserver` can return `null` for certain properties (e.g., `rootBounds` when root is viewport). Use `Type | Null` in Scala.js facades for accuracy.

3. **Avoiding null variables**: Use `lazy val` instead of `var x = null` followed by immediate assignment when the value references itself (like recursive observer callbacks).

4. **Unused parameters**: Remove unused parameters like `localContext` in Portal handlers to keep code clean.
