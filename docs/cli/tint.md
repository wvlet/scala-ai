# Terminal Styling

Tint provides a fluent API for styling terminal text with colors and modifiers.

## Basic Usage

```scala
import wvlet.uni.cli.Tint

// Single color
println(Tint.red("Error message"))
println(Tint.green("Success!"))
println(Tint.blue("Information"))

// With modifiers
println(Tint.red.bold("Bold red"))
println(Tint.yellow.underline("Underlined"))
println(Tint.cyan.italic("Italic text"))
```

## Colors

### Foreground Colors

```scala
Tint.black("text")
Tint.red("text")
Tint.green("text")
Tint.yellow("text")
Tint.blue("text")
Tint.magenta("text")
Tint.cyan("text")
Tint.white("text")
Tint.gray("text")
```

### Bright Colors

```scala
Tint.brightRed("text")
Tint.brightGreen("text")
Tint.brightYellow("text")
Tint.brightBlue("text")
Tint.brightMagenta("text")
Tint.brightCyan("text")
Tint.brightWhite("text")
```

### Background Colors

```scala
Tint.bgRed("text")
Tint.bgGreen("text")
Tint.bgYellow("text")
Tint.bgBlue("text")
Tint.bgMagenta("text")
Tint.bgCyan("text")
Tint.bgWhite("text")
```

## Text Modifiers

```scala
Tint.bold("Bold text")
Tint.dim("Dimmed text")
Tint.italic("Italic text")
Tint.underline("Underlined text")
Tint.strikethrough("Strikethrough")
Tint.inverse("Inverted colors")
Tint.hidden("Hidden text")
```

## Chaining Styles

Combine multiple styles:

```scala
Tint.red.bold("Red and bold")
Tint.green.underline("Green underlined")
Tint.yellow.bgBlue.bold("Yellow on blue, bold")
Tint.cyan.italic.underline("Cyan italic underlined")
```

## Extended Colors

### RGB Colors

```scala
Tint.rgb(255, 87, 51)("Custom RGB")
Tint.bgRgb(100, 149, 237)("Custom background")
```

### Hex Colors

```scala
Tint.hex("#FF5733")("Orange-red")
Tint.hex("#00FF00")("Lime green")
Tint.bgHex("#663399")("Purple background")
```

### 256-Color Palette

```scala
Tint.ansi256(208)("Color 208")
Tint.bgAnsi256(57)("Background 57")
```

## Reusable Styles

Create style presets:

```scala
val errorStyle = Tint.red.bold
val successStyle = Tint.green
val warningStyle = Tint.yellow.bold
val infoStyle = Tint.cyan

println(errorStyle("Failed to connect"))
println(successStyle("Operation completed"))
println(warningStyle("Disk space low"))
println(infoStyle("Processing..."))
```

## Disabling Colors

For non-TTY output or CI environments:

```scala
val style = Tint.red.bold

// Disable for this style
val plain = style.noColor
println(plain("No colors"))

// Or conditionally
val isInteractive = System.console() != null
val tint = if isInteractive then Tint.red else Tint().noColor
```

## Example: CLI Output

```scala
import wvlet.uni.cli.Tint

object MyCLI:
  val error = Tint.red.bold
  val success = Tint.green
  val highlight = Tint.cyan.bold
  val dim = Tint.gray

  def printStatus(name: String, status: String, ok: Boolean): Unit =
    val statusStyle = if ok then success else error
    val symbol = if ok then "✓" else "✗"
    println(s"${dim("[")}${statusStyle(symbol)}${dim("]")} ${name}: ${statusStyle(status)}")

  def main(args: Array[String]): Unit =
    printStatus("Database", "Connected", true)
    printStatus("Cache", "Timeout", false)
    printStatus("API", "Ready", true)
```

Output:
```
[✓] Database: Connected
[✗] Cache: Timeout
[✓] API: Ready
```

## Best Practices

1. **Define styles once** and reuse them
2. **Detect terminal** support before using colors
3. **Provide plain fallback** for non-TTY environments
4. **Use semantic names** (errorStyle, not redBold)
5. **Keep it readable** - don't overuse colors
