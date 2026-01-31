# Uni.dom Improvements Plan

## Summary

Add CSS Style DSL and CSS Class utilities to uni.dom to improve developer experience and type safety.

## Scope

Based on gap analysis comparing uni.dom with Laminar/ScalaTags, focusing on high-impact, practical improvements:

1. **CSS Style DSL** - Type-safe inline styles
2. **CSS Class Utilities** - Toggle and conditional class application with `when` syntax

## Implementation

### 1. CSS Style DSL (`CssStyles.scala`)

Create type-safe CSS style properties. The existing renderer already handles `style` attributes specially (DomRenderer.scala:417), so we generate `DomAttribute("style", "name: value;", append = true)`.

```scala
// New file: uni/.js/src/main/scala/wvlet/uni/dom/CssStyles.scala
package wvlet.uni.dom

case class StyleProperty(name: String):
  def :=(value: String): DomAttribute =
    DomAttribute("style", s"${name}: ${value};", append = true)

object CssStyles:
  // Layout
  val display = StyleProperty("display")
  val position = StyleProperty("position")
  val overflow = StyleProperty("overflow")
  // ... 60+ common CSS properties
```

**Usage**:
```scala
import wvlet.uni.dom.all.*
import wvlet.uni.dom.CssStyles.*

div(
  display := "flex",
  gap := "8px",
  color := "blue"
)
// Renders: <div style="display: flex; gap: 8px; color: blue;"></div>
```

### 2. CSS Class Utilities (`CssClass.scala`)

Add class toggle with `when` syntax for both static and reactive conditions.

```scala
// New file: uni/.js/src/main/scala/wvlet/uni/dom/CssClass.scala
package wvlet.uni.dom

import wvlet.uni.rx.{Rx, RxOps}

case class ClassToggle(className: String):
  // Static toggle - returns DomNode (either attribute or empty)
  def when(condition: Boolean): DomNode =
    if condition then DomAttribute("class", className, append = true)
    else DomNode.empty

  // Reactive toggle - returns Rx that emits class or empty
  def when[A](rx: RxOps[A])(using ev: A =:= Boolean): DomNode =
    rx.map(v => when(ev(v)))

object CssClass:
  def toggle(className: String): ClassToggle = ClassToggle(className)
```

**Modify HtmlAttrs.scala** - Add toggle method to cls:
```scala
// Change cls from lazy val to object
object cls extends DomAttributeOf("class"):
  def toggle(className: String): ClassToggle = ClassToggle(className)
```

**Usage**:
```scala
val isActive = Rx.variable(false)
val isDisabled = true

div(
  cls := "base-class",
  cls.toggle("active") when isActive,      // reactive
  cls.toggle("disabled") when isDisabled   // static
)

// Toggle the reactive state
isActive.update(!_)
```

## Files to Create/Modify

| File | Action |
|------|--------|
| `uni/.js/src/main/scala/wvlet/uni/dom/CssStyles.scala` | Create - Style property DSL |
| `uni/.js/src/main/scala/wvlet/uni/dom/CssClass.scala` | Create - ClassToggle helper |
| `uni/.js/src/main/scala/wvlet/uni/dom/HtmlAttrs.scala` | Modify - Change `cls` to object with `toggle` method |
| `uni/.js/src/main/scala/wvlet/uni/dom/all.scala` | Modify - Export CssStyles.* |
| `uni-dom-test/src/test/scala/wvlet/uni/dom/CssStylesTest.scala` | Create - Tests |
| `uni-dom-test/src/test/scala/wvlet/uni/dom/CssClassTest.scala` | Create - Tests |

**No changes needed to**:
- `DomAttribute.scala` - existing structure works
- `DomRenderer.scala` - already handles style attributes with append mode

## CSS Properties to Include

### Layout (10)
display, position, top, right, bottom, left, zIndex, overflow, overflowX, overflowY

### Box Model (12)
width, height, minWidth, minHeight, maxWidth, maxHeight, margin, marginTop, marginRight, marginBottom, marginLeft, padding (same pattern)

### Flexbox (8)
flexDirection, flexWrap, justifyContent, alignItems, alignContent, flex, flexGrow, flexShrink, gap

### Grid (6)
gridTemplateColumns, gridTemplateRows, gridColumn, gridRow, gridGap, placeItems

### Typography (8)
fontSize, fontWeight, fontFamily, fontStyle, lineHeight, textAlign, textDecoration, letterSpacing

### Colors & Background (6)
color, backgroundColor, background, backgroundImage, backgroundSize, backgroundPosition

### Border (8)
border, borderWidth, borderStyle, borderColor, borderRadius, borderTop, borderBottom, outline

### Transform & Animation (4)
transform, transition, animation, opacity

### Other (3)
cursor, visibility, pointerEvents

**Total: ~65 CSS properties**

## Verification

1. **Compile**: `./sbt uniJS/compile`
2. **Run tests**: `./sbt "uniDomTest/test"`
3. **Format**: `./sbt scalafmtAll`

### Test Cases

**CssStylesTest.scala**:
```scala
test("style properties generate correct attributes") {
  val elem = div(display := "flex", color := "red")
  val html = DomRenderer.renderToHtml(elem.render)
  html shouldContain "display: flex"
  html shouldContain "color: red"
}

test("multiple styles are combined") {
  val elem = div(gap := "8px", padding := "16px")
  // Should produce style="gap: 8px; padding: 16px;"
}
```

**CssClassTest.scala**:
```scala
test("static class toggle when true") {
  val elem = div(cls.toggle("active") when true)
  val html = DomRenderer.renderToHtml(elem.render)
  html shouldContain "active"
}

test("static class toggle when false") {
  val elem = div(cls.toggle("active") when false)
  val html = DomRenderer.renderToHtml(elem.render)
  html shouldNotContain "active"
}

test("reactive class toggle") {
  val isActive = Rx.variable(false)
  val elem = div(cls.toggle("active") when isActive)
  // Initial: no class
  // After isActive := true: class="active"
}
```

## Out of Scope (Future Work)

- DOM Properties (value, checked) - separate PR
- Keyed collections / split operator - separate PR
- Two-way binding - separate PR
- ARIA attributes - separate PR
