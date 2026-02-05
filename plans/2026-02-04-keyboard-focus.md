# Keyboard Shortcuts & Focus Management Plan

## Overview

Add two accessibility-focused features to uni-dom:
1. **Keyboard Shortcuts** - Global and scoped keyboard shortcut management
2. **Focus Management** - Focus traps, auto-focus, and focus restoration

## API Design

### 1. Keyboard Shortcuts

```scala
object Keyboard:
  // Global shortcuts
  def bind(shortcut: String, handler: () => Unit): Cancelable
  def bindAll(bindings: (String, () => Unit)*): Cancelable

  // Scoped shortcuts (element modifier)
  def scoped(bindings: (String, () => Unit)*): DomNode

  // Reactive key state
  def isPressed(key: String): Rx[Boolean]
  def modifiers: Rx[Modifiers]

case class Modifiers(
    ctrl: Boolean,
    alt: Boolean,
    shift: Boolean,
    meta: Boolean
)
```

**Shortcut String Format:**
- `"escape"` - Single key
- `"ctrl+s"` - Modifier + key
- `"ctrl+shift+p"` - Multiple modifiers
- `"meta+k"` - Command key (Mac) / Windows key

**Usage:**
```scala
// Global shortcuts
val cancel = Keyboard.bind("ctrl+s", () => save())
val cancel2 = Keyboard.bindAll(
  "ctrl+s" -> (() => save()),
  "escape" -> (() => closeModal())
)

// Scoped shortcuts (only when element focused)
input(
  Keyboard.scoped(
    "enter" -> (() => submit()),
    "escape" -> (() => cancel())
  )
)

// Reactive state
Keyboard.isPressed("shift").map { pressed =>
  if pressed then "Multi-select mode" else "Single select"
}
```

### 2. Focus Management

```scala
object Focus:
  // Focus trap - tab cycles within children
  def trap(children: DomNode*): RxElement

  // Auto-focus on mount
  def onMount: DomNode
  def onMountDelayed(delayMs: Int = 0): DomNode

  // Focus restoration
  def withRestoration[A](f: (() => Unit) => A): A

  // Utilities
  def active: Rx[Option[dom.Element]]
  def focusById(id: String): Unit
  def blur(): Unit
```

**Usage:**
```scala
// Modal with focus trap
val isOpen = Rx.variable(false)
isOpen.map { open =>
  if open then
    Focus.trap(
      div(cls -> "modal",
        input(Focus.onMount, placeholder -> "Email"),
        button("Submit"),
        button(onclick -> (() => isOpen := false), "Close")
      )
    )
  else DomNode.empty
}

// Focus restoration
Focus.withRestoration { restoreFocus =>
  showModal()
  button(onclick -> { () =>
    closeModal()
    restoreFocus()
  }, "Close")
}
```

## Implementation Details

### Keyboard: Global Listener Pattern

Follow `WindowScroll.scala` pattern:

```scala
private class KeyboardState extends Cancelable:
  private val pressedKeys = Rx.variable(Set.empty[String])
  private val bindings = mutable.Map[String, () => Unit]()

  private val keydownHandler: js.Function1[dom.KeyboardEvent, Unit] = e =>
    val combo = buildCombo(e)
    pressedKeys.update(_ + e.key.toLowerCase)
    bindings.get(combo).foreach { handler =>
      e.preventDefault()
      handler()
    }

  private val keyupHandler: js.Function1[dom.KeyboardEvent, Unit] = e =>
    pressedKeys.update(_ - e.key.toLowerCase)

  dom.window.addEventListener("keydown", keydownHandler)
  dom.window.addEventListener("keyup", keyupHandler)

  override def cancel: Unit =
    dom.window.removeEventListener("keydown", keydownHandler)
    dom.window.removeEventListener("keyup", keyupHandler)
```

### Keyboard: Shortcut Parsing

```scala
case class KeyCombination(
    key: String,           // "s", "escape", "enter"
    ctrl: Boolean = false,
    alt: Boolean = false,
    shift: Boolean = false,
    meta: Boolean = false
)

def parse(shortcut: String): KeyCombination =
  val parts = shortcut.toLowerCase.split("\\+")
  KeyCombination(
    key = parts.last,
    ctrl = parts.contains("ctrl"),
    alt = parts.contains("alt"),
    shift = parts.contains("shift"),
    meta = parts.contains("meta") || parts.contains("cmd")
  )
```

### Focus: Trap Implementation

```scala
private case class FocusTrap(children: Seq[DomNode]) extends RxElement:
  private var firstFocusable: Option[dom.Element] = None
  private var lastFocusable: Option[dom.Element] = None

  override def onMount(node: Any): Unit =
    val container = node.asInstanceOf[dom.Element]
    val focusables = getFocusableElements(container)
    firstFocusable = focusables.headOption
    lastFocusable = focusables.lastOption
    firstFocusable.foreach(_.asInstanceOf[dom.HTMLElement].focus())

  override def render: RxElement =
    div(
      tabindex -> "-1",
      onkeydown -> { (e: dom.KeyboardEvent) =>
        if e.key == "Tab" then
          if e.shiftKey then
            // Shift+Tab: wrap to last if at first
            if dom.document.activeElement == firstFocusable.orNull then
              e.preventDefault()
              lastFocusable.foreach(_.asInstanceOf[dom.HTMLElement].focus())
          else
            // Tab: wrap to first if at last
            if dom.document.activeElement == lastFocusable.orNull then
              e.preventDefault()
              firstFocusable.foreach(_.asInstanceOf[dom.HTMLElement].focus())
      },
      children*
    )

def getFocusableElements(container: dom.Element): Seq[dom.Element] =
  val selector = "button, [href], input, select, textarea, [tabindex]:not([tabindex=\"-1\"])"
  container.querySelectorAll(selector).toSeq
```

### Focus: onMount Modifier

```scala
case class FocusOnMount(delay: Int = 0) extends RxElement:
  override def onMount(node: Any): Unit =
    node match
      case elem: dom.HTMLElement =>
        if delay > 0 then
          dom.window.setTimeout(() => elem.focus(), delay)
        else
          elem.focus()
      case _ => ()

  override def render: RxElement = DomNode.empty
```

## Files to Create

| File | Description |
|------|-------------|
| `uni/.js/src/main/scala/wvlet/uni/dom/Keyboard.scala` | Keyboard shortcuts implementation |
| `uni/.js/src/main/scala/wvlet/uni/dom/Focus.scala` | Focus management implementation |
| `uni-dom-test/src/test/scala/wvlet/uni/dom/KeyboardTest.scala` | Keyboard tests |
| `uni-dom-test/src/test/scala/wvlet/uni/dom/FocusTest.scala` | Focus tests |

## Files to Modify

| File | Change |
|------|--------|
| `uni/.js/src/main/scala/wvlet/uni/dom/all.scala` | Export Keyboard, Focus, Modifiers |

## Patterns to Follow

- `WindowScroll.scala` - Global listener with cleanup
- `NetworkStatus.scala` - Simple reactive state
- `DomRef.scala` - Focus/blur methods already exist
- `RxElement.scala` - Lifecycle hooks (onMount, beforeUnmount)

## Verification

```bash
./sbt "uniJS/compile"
./sbt "domTest/testOnly *KeyboardTest"
./sbt "domTest/testOnly *FocusTest"
./sbt scalafmtAll
```

## Edge Cases

1. **Multiple bindings for same shortcut** - Last one wins
2. **Scoped shortcuts with focus** - Only fire when element has focus
3. **Focus trap with no focusable elements** - Container itself gets focus
4. **Browser default shortcuts** - `preventDefault()` to override (e.g., Ctrl+S)
5. **Mac vs Windows** - `meta` = Cmd on Mac, Windows key on Windows
