# CLI Utilities

uni provides utilities for building command-line applications with styled output, progress indicators, and command launching.

## Overview

| Component | Description |
|-----------|-------------|
| [Terminal Styling](./tint) | Colorful terminal output with Tint |
| [Progress Indicators](./progress) | Progress bars and spinners |
| [Command Launcher](./launcher) | Type-safe CLI argument parsing |

## Quick Start

### Colored Output

```scala
import wvlet.uni.cli.Tint

println(Tint.green("Success!"))
println(Tint.red.bold("Error!"))
println(Tint.yellow.bgBlue("Warning"))
```

### Progress Bar

```scala
import wvlet.uni.cli.ProgressBar

val bar = ProgressBar(total = 100).start()
for i <- 1 to 100 do
  Thread.sleep(50)
  bar.increment()
bar.finish()
```

### Command Launcher

```scala
import wvlet.uni.cli.launcher.Launcher

case class MyApp(
  name: String = "world",
  verbose: Boolean = false
):
  def run(): Unit =
    if verbose then println(s"Running with name: ${name}")
    println(s"Hello, ${name}!")

// Parse args and run
Launcher.execute[MyApp](args)
```

## Features

- **Cross-Platform** - Works on JVM and Scala Native
- **Automatic Detection** - Detects terminal capabilities
- **Fluent API** - Method chaining for styling
- **Type-Safe** - Compile-time argument validation

## Package

```scala
import wvlet.uni.cli.*
import wvlet.uni.cli.launcher.*
```
