# uni-dom Window Events Implementation Plan

## Overview

Add reactive window event bindings for scroll tracking, visibility state, and window dimensions to uni-dom. Follows the established patterns from MediaQuery and NetworkStatus.

## Components

### 1. WindowScroll - Scroll Position Tracking

Reactive scroll position with built-in throttling for performance.

**API:**
```scala
object WindowScroll:
  def x: Double                      // Current horizontal scroll
  def y: Double                      // Current vertical scroll
  def scrollX: Rx[Double]            // Reactive horizontal position
  def scrollY: Rx[Double]            // Reactive vertical position
  def scroll: Rx[(Double, Double)]   // Combined (x, y) stream
  def stop(): Unit                   // Cleanup
```

**Usage:**
```scala
// Show "back to top" button when scrolled
div(
  WindowScroll.scrollY.map { y =>
    if y > 500 then button("Back to Top") else DomNode.empty
  }
)
```

**Implementation notes:**
- Uses 16ms throttle (~60fps) for performance
- Leading + trailing edge throttling ensures final position is captured
- Private inner class with lazy val singleton

### 2. WindowVisibility - Document Visibility State

Track whether the page is visible or hidden (tab switching, minimizing).

**API:**
```scala
object WindowVisibility:
  def isVisible: Boolean             // Current visibility
  def state: String                  // "visible", "hidden", etc.
  def visible: Rx[Boolean]           // Reactive visibility
  def hidden: Rx[Boolean]            // Inverse of visible
  def visibilityState: Rx[String]    // Reactive state string
  def stop(): Unit                   // Cleanup
```

**Usage:**
```scala
// Pause video when tab is hidden
WindowVisibility.visible.run { visible =>
  if visible then video.play() else video.pause()
}
```

### 3. WindowDimensions - Window Size Tracking

Reactive window dimensions with resize tracking.

**API:**
```scala
object WindowDimensions:
  def innerWidth: Int                // Viewport width
  def innerHeight: Int               // Viewport height
  def outerWidth: Int                // Full window width
  def outerHeight: Int               // Full window height
  def rxInnerWidth: Rx[Int]          // Reactive viewport width
  def rxInnerHeight: Rx[Int]         // Reactive viewport height
  def rxOuterWidth: Rx[Int]          // Reactive window width
  def rxOuterHeight: Rx[Int]         // Reactive window height
  def dimensions: Rx[(Int, Int)]     // Combined (width, height)
  def stop(): Unit                   // Cleanup
```

**Usage:**
```scala
// Responsive layout without MediaQuery
div(
  WindowDimensions.rxInnerWidth.map { width =>
    if width < 768 then "mobile-layout" else "desktop-layout"
  }
)
```

## Files to Create

| File | Description |
|------|-------------|
| `uni/.js/src/main/scala/wvlet/uni/dom/WindowScroll.scala` | Scroll position tracking |
| `uni/.js/src/main/scala/wvlet/uni/dom/WindowVisibility.scala` | Document visibility state |
| `uni/.js/src/main/scala/wvlet/uni/dom/WindowDimensions.scala` | Window dimensions tracking |
| `uni-dom-test/src/test/scala/wvlet/uni/dom/WindowScrollTest.scala` | Scroll tests |
| `uni-dom-test/src/test/scala/wvlet/uni/dom/WindowVisibilityTest.scala` | Visibility tests |
| `uni-dom-test/src/test/scala/wvlet/uni/dom/WindowDimensionsTest.scala` | Dimensions tests |

## Files to Modify

| File | Changes |
|------|---------|
| `uni/.js/src/main/scala/wvlet/uni/dom/all.scala` | Add exports for WindowScroll, WindowVisibility, WindowDimensions |

## Implementation Pattern

Follow the NetworkStatus pattern:

```scala
object WindowXxx:
  private class XxxVar extends Cancelable:
    private val underlying = Rx.variable(initialValue)
    private val listener: js.Function1[dom.Event, Unit] = (_: dom.Event) =>
      underlying := newValue

    dom.window.addEventListener("event", listener)

    def get: T = underlying.get
    def rx: Rx[T] = underlying

    override def cancel: Unit =
      dom.window.removeEventListener("event", listener)

  private lazy val instance: XxxVar = XxxVar()

  def value: T = instance.get
  def rxValue: Rx[T] = instance.rx
  def stop(): Unit = instance.cancel
```

## Scroll Throttling Strategy

```scala
private var lastUpdateTime: Double = 0
private var scheduledUpdate: js.UndefOr[Int] = js.undefined

private val listener: js.Function1[dom.Event, Unit] = (_: dom.Event) =>
  val now = js.Date.now()
  if now - lastUpdateTime >= throttleMs then
    // Leading edge: update immediately
    lastUpdateTime = now
    updateValues()
  else if scheduledUpdate.isEmpty then
    // Trailing edge: schedule final update
    scheduledUpdate = dom.window.setTimeout(
      () => {
        scheduledUpdate = js.undefined
        lastUpdateTime = js.Date.now()
        updateValues()
      },
      throttleMs - (now - lastUpdateTime).toInt
    )
```

## Implementation Order

1. WindowVisibility (simplest, follows NetworkStatus exactly)
2. WindowDimensions (similar, just more RxVars)
3. WindowScroll (most complex due to throttling)
4. Update all.scala exports
5. Create tests

## Verification

```bash
# In worktree
cd /Users/leo/work/uni/.worktree/feature-window-events

# Compile
./sbt compile

# Run tests
./sbt "domTestJS/test"

# Format
./sbt scalafmtAll
```

## References

Pattern files to follow:
- `uni/.js/src/main/scala/wvlet/uni/dom/NetworkStatus.scala` - Primary pattern
- `uni/.js/src/main/scala/wvlet/uni/dom/MediaQuery.scala` - Secondary pattern
- `uni-dom-test/src/test/scala/wvlet/uni/dom/NetworkStatusTest.scala` - Test pattern
