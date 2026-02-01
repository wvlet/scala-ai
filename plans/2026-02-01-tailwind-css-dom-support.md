# Tailwind CSS Support for uni-dom

## Overview

Design a seamless Tailwind CSS integration for uni-dom that enables:
1. Easy class composition with a fluent API
2. Reusable style definitions (similar to `@apply`)
3. Conditional/dynamic class application
4. Reactive class updates via `Rx`
5. Type-safe modifiers for common patterns

## Design Goals

- **Minimal overhead**: No runtime CSS generation, just class manipulation
- **Composable**: Styles can be combined and extended
- **Type-safe**: Compile-time checking where possible
- **Idiomatic Scala**: Use Scala 3 features (given, extension methods, enums)
- **Reactive-first**: Integrate with existing Rx system

## API Design

### 1. TwStyle - Reusable Style Definitions

```scala
// Define reusable styles
val buttonBase = tw("px-4 py-2 rounded font-medium")
val buttonPrimary = tw("bg-blue-500 text-white hover:bg-blue-600")
val buttonSecondary = tw("bg-gray-200 text-gray-800 hover:bg-gray-300")

// Compose styles
val primaryButton = buttonBase + buttonPrimary
val secondaryButton = buttonBase + buttonSecondary

// Use in elements
button(primaryButton, "Click me")
div(tw("flex items-center gap-4"), ...)
```

### 2. Conditional Classes

```scala
// Using when/unless
val isActive = Rx.variable(false)
div(
  tw("px-4 py-2"),
  tw.when(isActive)("bg-blue-500 text-white"),
  tw.unless(isActive)("bg-gray-100 text-gray-600")
)

// Using match expressions
val status: Rx[Status] = ...
div(
  tw("p-4 rounded"),
  status.map {
    case Status.Success => tw("bg-green-100 text-green-800")
    case Status.Error   => tw("bg-red-100 text-red-800")
    case Status.Pending => tw("bg-yellow-100 text-yellow-800")
  }
)

// Boolean toggle
button(
  tw("px-4 py-2 rounded"),
  tw.toggle(isDisabled, "opacity-50 cursor-not-allowed", "hover:bg-blue-600")
)
```

### 3. Variant Support (Responsive/State)

```scala
// Responsive variants
div(
  tw("text-sm md:text-base lg:text-lg"),
  tw.sm("hidden").md("block").lg("flex")
)

// State variants via method chaining
button(
  tw("bg-blue-500")
    .hover("bg-blue-600")
    .focus("ring-2 ring-blue-300")
    .active("bg-blue-700")
    .disabled("opacity-50 cursor-not-allowed")
)

// Dark mode
div(
  tw("bg-white text-gray-900")
    .dark("bg-gray-900 text-white")
)
```

### 4. Class Groups (Semantic Grouping)

```scala
// Define a component style group
object ButtonStyles:
  val base = tw("inline-flex items-center justify-center font-medium rounded transition-colors")
  val sm = tw("px-3 py-1.5 text-sm")
  val md = tw("px-4 py-2 text-base")
  val lg = tw("px-6 py-3 text-lg")

  val primary = tw("bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500")
  val secondary = tw("bg-gray-100 text-gray-900 hover:bg-gray-200 focus:ring-gray-500")
  val danger = tw("bg-red-600 text-white hover:bg-red-700 focus:ring-red-500")

// Usage
button(ButtonStyles.base + ButtonStyles.md + ButtonStyles.primary, "Save")
```

### 5. Class List Builder

```scala
// For complex conditional logic
val classes = TwClasses()
  .add("flex items-center")
  .addIf(isLarge, "text-lg")
  .addUnless(isCompact, "gap-4")
  .addMatch(variant) {
    case "primary"   => "bg-blue-500"
    case "secondary" => "bg-gray-500"
    case _           => "bg-white"
  }
  .build()

div(classes, ...)
```

## Implementation

### Core Types

```scala
// TwStyle.scala
package wvlet.uni.dom

import wvlet.airframe.rx.Rx

/**
 * A composable Tailwind CSS style definition
 */
case class TwStyle(classes: String) extends DomNode:
  def +(other: TwStyle): TwStyle = TwStyle(s"${classes} ${other.classes}".trim)
  def ++(others: TwStyle*): TwStyle = others.foldLeft(this)(_ + _)

  // State variants
  def hover(cls: String): TwStyle = addVariant("hover", cls)
  def focus(cls: String): TwStyle = addVariant("focus", cls)
  def active(cls: String): TwStyle = addVariant("active", cls)
  def disabled(cls: String): TwStyle = addVariant("disabled", cls)
  def group(cls: String): TwStyle = addVariant("group-hover", cls)

  // Dark mode
  def dark(cls: String): TwStyle = addVariant("dark", cls)

  // Responsive variants
  def sm(cls: String): TwStyle = addVariant("sm", cls)
  def md(cls: String): TwStyle = addVariant("md", cls)
  def lg(cls: String): TwStyle = addVariant("lg", cls)
  def xl(cls: String): TwStyle = addVariant("xl", cls)
  def xxl(cls: String): TwStyle = addVariant("2xl", cls)

  private def addVariant(variant: String, cls: String): TwStyle =
    val prefixed = cls.split("\\s+").map(c => s"${variant}:${c}").mkString(" ")
    TwStyle(s"${classes} ${prefixed}".trim)

object TwStyle:
  val empty: TwStyle = TwStyle("")

  def apply(classes: String): TwStyle = new TwStyle(classes.trim)

  // Conditional styles
  def when(condition: Boolean)(classes: String): TwStyle =
    if condition then TwStyle(classes) else empty

  def when(condition: Rx[Boolean])(classes: String): Rx[TwStyle] =
    condition.map(c => if c then TwStyle(classes) else empty)

  def unless(condition: Boolean)(classes: String): TwStyle =
    if !condition then TwStyle(classes) else empty

  def toggle(condition: Boolean, whenTrue: String, whenFalse: String): TwStyle =
    if condition then TwStyle(whenTrue) else TwStyle(whenFalse)

  def toggle(condition: Rx[Boolean], whenTrue: String, whenFalse: String): Rx[TwStyle] =
    condition.map(c => if c then TwStyle(whenTrue) else TwStyle(whenFalse))
```

### Class Builder

```scala
// TwClasses.scala
package wvlet.uni.dom

/**
 * A mutable builder for complex conditional class lists
 */
class TwClasses:
  private val parts = List.newBuilder[String]

  def add(classes: String): TwClasses =
    parts += classes
    this

  def addIf(condition: Boolean, classes: String): TwClasses =
    if condition then parts += classes
    this

  def addUnless(condition: Boolean, classes: String): TwClasses =
    if !condition then parts += classes
    this

  def addOption(opt: Option[String]): TwClasses =
    opt.foreach(parts += _)
    this

  def addMatch[T](value: T)(pf: PartialFunction[T, String]): TwClasses =
    if pf.isDefinedAt(value) then parts += pf(value)
    this

  def build(): TwStyle = TwStyle(parts.result().mkString(" "))

object TwClasses:
  def apply(): TwClasses = new TwClasses
```

### Integration with DomElement

```scala
// In Embedding.scala - add TwStyle support
given twStyleEmbeddable: EmbeddableNode[TwStyle] with
  override def toNode(a: TwStyle): DomNode =
    DomAttribute("class", a.classes, append = true)

given rxTwStyleEmbeddable: EmbeddableNode[Rx[TwStyle]] with
  override def toNode(a: Rx[TwStyle]): DomNode =
    DomAttribute("class", a.map(_.classes), append = true)
```

### Convenience Function

```scala
// In all.scala
def tw(classes: String): TwStyle = TwStyle(classes)

// Allow varargs for multiple class strings
def tw(first: String, rest: String*): TwStyle =
  TwStyle((first +: rest).mkString(" "))
```

## Usage Examples

### Basic Usage

```scala
import wvlet.uni.dom.all.*

// Simple usage
div(tw("flex items-center justify-between p-4"))

// Combining styles
val card = tw("bg-white rounded-lg shadow-md")
val cardHeader = tw("px-6 py-4 border-b")
val cardBody = tw("px-6 py-4")

div(card,
  div(cardHeader, h2("Title")),
  div(cardBody, p("Content"))
)
```

### Component Styles

```scala
object Styles:
  // Button variants
  object Button:
    val base = tw("inline-flex items-center justify-center font-medium rounded-md transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2")
    val sm = tw("px-3 py-1.5 text-sm")
    val md = tw("px-4 py-2 text-base")
    val lg = tw("px-6 py-3 text-lg")
    val primary = tw("bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500")
    val secondary = tw("bg-gray-100 text-gray-700 hover:bg-gray-200 focus:ring-gray-500")
    val danger = tw("bg-red-600 text-white hover:bg-red-700 focus:ring-red-500")

  // Input styles
  object Input:
    val base = tw("block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500")
    val sm = tw("px-3 py-1.5 text-sm")
    val md = tw("px-4 py-2 text-base")
    val error = tw("border-red-500 focus:border-red-500 focus:ring-red-500")

// Usage
button(Styles.Button.base + Styles.Button.md + Styles.Button.primary, "Save")
input(Styles.Input.base + Styles.Input.md, placeholder -> "Enter text")
```

### Reactive Styles

```scala
val isLoading = Rx.variable(false)
val theme = Rx.variable[Theme](Theme.Light)

button(
  Styles.Button.base + Styles.Button.md + Styles.Button.primary,
  tw.when(isLoading)("opacity-50 cursor-wait"),
  tw.unless(isLoading)("hover:bg-blue-700"),
  onclick -> { () => isLoading := true },
  if isLoading.get then "Loading..." else "Submit"
)

// Theme-based styling
div(
  theme.map {
    case Theme.Light => tw("bg-white text-gray-900")
    case Theme.Dark  => tw("bg-gray-900 text-white")
  },
  "Content"
)
```

### Complex Conditional Styles

```scala
def statusBadge(status: Status) =
  val classes = TwClasses()
    .add("px-2 py-1 rounded-full text-xs font-medium")
    .addMatch(status) {
      case Status.Active   => "bg-green-100 text-green-800"
      case Status.Pending  => "bg-yellow-100 text-yellow-800"
      case Status.Inactive => "bg-gray-100 text-gray-500"
      case Status.Error    => "bg-red-100 text-red-800"
    }
    .build()

  span(classes, status.toString)
```

## Files to Create/Modify

1. **Create**: `uni/.js/src/main/scala/wvlet/uni/dom/TwStyle.scala`
2. **Create**: `uni/.js/src/main/scala/wvlet/uni/dom/TwClasses.scala`
3. **Modify**: `uni/.js/src/main/scala/wvlet/uni/dom/Embedding.scala` - Add TwStyle embedding
4. **Modify**: `uni/.js/src/main/scala/wvlet/uni/dom/all.scala` - Export tw function
5. **Create**: `uni-dom-test/src/test/scala/wvlet/uni/dom/TwStyleTest.scala` - Tests

## Benefits

1. **Clean separation**: Styles are first-class citizens, separate from elements
2. **Reusability**: Define once, use everywhere
3. **Composability**: Combine styles with `+` operator
4. **Type-safety**: TwStyle is a proper type, not just strings
5. **Reactive**: Seamless integration with Rx for dynamic styling
6. **No runtime overhead**: Just class string manipulation
7. **IDE-friendly**: Method completion for variants
