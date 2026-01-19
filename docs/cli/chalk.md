# Terminal Styling

Chalk provides a fluent API for styling terminal text with colors and modifiers.

## Basic Usage

```scala
import wvlet.uni.cli.Chalk

// Single color
println(Chalk.red("Error message"))
println(Chalk.green("Success!"))
println(Chalk.blue("Information"))

// With modifiers
println(Chalk.red.bold("Bold red"))
println(Chalk.yellow.underline("Underlined"))
println(Chalk.cyan.italic("Italic text"))
```

## Colors

### Foreground Colors

```scala
Chalk.black("text")
Chalk.red("text")
Chalk.green("text")
Chalk.yellow("text")
Chalk.blue("text")
Chalk.magenta("text")
Chalk.cyan("text")
Chalk.white("text")
Chalk.gray("text")
```

### Bright Colors

```scala
Chalk.brightRed("text")
Chalk.brightGreen("text")
Chalk.brightYellow("text")
Chalk.brightBlue("text")
Chalk.brightMagenta("text")
Chalk.brightCyan("text")
Chalk.brightWhite("text")
```

### Background Colors

```scala
Chalk.bgRed("text")
Chalk.bgGreen("text")
Chalk.bgYellow("text")
Chalk.bgBlue("text")
Chalk.bgMagenta("text")
Chalk.bgCyan("text")
Chalk.bgWhite("text")
```

## Text Modifiers

```scala
Chalk.bold("Bold text")
Chalk.dim("Dimmed text")
Chalk.italic("Italic text")
Chalk.underline("Underlined text")
Chalk.strikethrough("Strikethrough")
Chalk.inverse("Inverted colors")
Chalk.hidden("Hidden text")
```

## Chaining Styles

Combine multiple styles:

```scala
Chalk.red.bold("Red and bold")
Chalk.green.underline("Green underlined")
Chalk.yellow.bgBlue.bold("Yellow on blue, bold")
Chalk.cyan.italic.underline("Cyan italic underlined")
```

## Extended Colors

### RGB Colors

```scala
Chalk.rgb(255, 87, 51)("Custom RGB")
Chalk.bgRgb(100, 149, 237)("Custom background")
```

### Hex Colors

```scala
Chalk.hex("#FF5733")("Orange-red")
Chalk.hex("#00FF00")("Lime green")
Chalk.bgHex("#663399")("Purple background")
```

### 256-Color Palette

```scala
Chalk.ansi256(208)("Color 208")
Chalk.bgAnsi256(57)("Background 57")
```

## Reusable Styles

Create style presets:

```scala
val errorStyle = Chalk.red.bold
val successStyle = Chalk.green
val warningStyle = Chalk.yellow.bold
val infoStyle = Chalk.cyan

println(errorStyle("Failed to connect"))
println(successStyle("Operation completed"))
println(warningStyle("Disk space low"))
println(infoStyle("Processing..."))
```

## Disabling Colors

For non-TTY output or CI environments:

```scala
val style = Chalk.red.bold

// Disable for this style
val plain = style.noColor
println(plain("No colors"))

// Or conditionally
val isInteractive = System.console() != null
val chalk = if isInteractive then Chalk.red else Chalk().noColor
```

## Example: CLI Output

```scala
import wvlet.uni.cli.Chalk

object MyCLI:
  val error = Chalk.red.bold
  val success = Chalk.green
  val highlight = Chalk.cyan.bold
  val dim = Chalk.gray

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
