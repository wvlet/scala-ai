# Cross-Platform CLI Launcher Library

## Overview

Create a command-line client library similar to airframe-launcher but using Scala annotations (instead of Java annotations) that work cross-platform via Surface's compile-time annotation extraction.

## Key Requirements

1. **Cross-platform**: JVM, Scala.js, Scala Native
2. **Scala annotations**: Use `extends StaticAnnotation` instead of Java `@interface`
3. **Surface integration**: Leverage existing compile-time annotation reading
4. **Features**: Commands, options, arguments, sub-commands, nested options, help generation
5. **Built-in help**: `-h,--help` automatically added to all commands by default
6. **Key-value pairs**: Support `-Lkey=value` style options for maps/properties

## Implementation Plan

### Phase 1: Extend Surface for Method Annotations

**Problem**: `ClassMethodSurface` currently lacks `annotations` field, needed for `@command` on methods.

**Files to modify**:
- `uni/src/main/scala/wvlet/uni/surface/Surfaces.scala` (line 308)
  - Add `annotations: Seq[Annotation] = Seq.empty` to `ClassMethodSurface`
  - Add helper methods `findAnnotation`, `hasAnnotation` to `MethodSurface` trait

- `uni/src/main/scala/wvlet/uni/surface/CompileTimeSurfaceFactory.scala` (line 1397)
  - Extract method annotations using existing `extractAnnotations(symbol)` method
  - Pass to `ClassMethodSurface` constructor

### Phase 2: Define Launcher Annotations and Types

**New file**: `uni/src/main/scala/wvlet/uni/cli/launcher/annotations.scala`

```scala
package wvlet.uni.cli.launcher

import scala.annotation.StaticAnnotation

class command(
    val description: String = "",
    val isDefault: Boolean = false,
    val usage: String = ""
) extends StaticAnnotation

class option(
    val prefix: String = "",
    val description: String = ""
) extends StaticAnnotation

class argument(
    val name: String = "",
    val description: String = ""
) extends StaticAnnotation
```

**New file**: `uni/src/main/scala/wvlet/uni/cli/launcher/KeyValue.scala`

```scala
package wvlet.uni.cli.launcher

/**
 * Represents a key=value pair from command line options.
 * Used with prefixed options like -Lkey=value, -Dkey=value, etc.
 */
case class KeyValue(key: String, value: String):
  override def toString: String = s"${key}=${value}"

object KeyValue:
  /**
   * Parse "key=value" string into KeyValue.
   * If no '=' is present, value defaults to empty string.
   */
  def parse(s: String): KeyValue =
    s.indexOf('=') match
      case -1  => KeyValue(s, "")
      case idx => KeyValue(s.substring(0, idx), s.substring(idx + 1))
```

### Phase 3: Core Launcher Classes

**New files in** `uni/src/main/scala/wvlet/uni/cli/launcher/`:

| File | Purpose |
|------|---------|
| `Launcher.scala` | Entry point: `Launcher.of[A]`, `execute(args)` |
| `CommandLauncher.scala` | Wraps single command, handles execution |
| `OptionParser.scala` | Parses CLI args using schema |
| `OptionSchema.scala` | Builds schema from Surface annotations |
| `CLOption.scala` | Option/argument item definitions |
| `ParseTree.scala` | Hierarchical parsed data (pure Scala) |
| `HelpPrinter.scala` | Help message generation using `Chalk` |
| `CommandLineTokenizer.scala` | Tokenizes command line strings |
| `KeyValue.scala` | Key-value pair type |

### Phase 4: Built-in Help Option

The `-h,--help` option is **automatically injected** into every command:

```scala
// In CommandLauncher or OptionSchema:
private val builtInHelpOption = CLOption(
  path = Path.current / "_help",
  prefixes = Seq("-h", "--help"),
  description = "Display this help message",
  param = None  // Virtual option, not mapped to a parameter
)

// Automatically prepended to all option lists
def allOptions: Seq[CLOption] = builtInHelpOption +: userDefinedOptions
```

**Behavior**:
- When `-h` or `--help` is detected, show help and exit (or return special result)
- Works at any command level (main command, sub-commands)
- No need for users to define help in their case classes

### Phase 5: Key-Value Pair Option Support

**Pattern**: `-<prefix><key>=<value>` (e.g., `-Lwvlet.log=debug`, `-Dfoo=bar`)

**Detection**: When parameter type is `KeyValue` or `Seq[KeyValue]`, the option uses key-value parsing:

```scala
// In OptionParser - detect key-value options by type
def isKeyValueOption(opt: CLOption): Boolean =
  val surface = opt.param.map(_.surface)
  surface.exists { s =>
    s.fullName == "wvlet.uni.cli.launcher.KeyValue" ||
    (s.isSeq && s.typeArgs.headOption.exists(_.fullName == "wvlet.uni.cli.launcher.KeyValue"))
  }

// Parsing: -Lkey=value
val KeyValuePattern = """^(-\w)(.+)$""".r  // e.g., -L followed by key=value

args match
  case KeyValuePattern(prefix, rest) if isKeyValuePrefix(prefix) =>
    val kv = KeyValue.parse(rest)  // Parse "key=value"
    // Add to accumulated key-values for this option
```

**Usage examples**:
```scala
case class MyApp(
    // Single key-value: -Lpackage=debug
    @option(prefix = "-L", description = "Set log level (package=level)")
    logLevel: Option[KeyValue] = None,

    // Multiple key-values: -Lfoo=debug -Lbar=info
    @option(prefix = "-L", description = "Set log levels")
    logLevels: Seq[KeyValue] = Seq.empty,

    // Java-style properties: -Dkey=value
    @option(prefix = "-D", description = "System property")
    properties: Seq[KeyValue] = Seq.empty
)

// Command line:
// myapp -Lwvlet.log=debug -Lcom.example=info -Dfoo=bar
```

**Help output**:
```
[options]
 -h, --help              Display this help message
 -L<key>=<value>         Set log levels
 -D<key>=<value>         System property
```

### Phase 6: Implementation Details

#### Launcher API
```scala
// Simple usage - help is automatic
case class MyApp(
    @option(prefix = "-v,--verbose", description = "Enable verbose output")
    verbose: Boolean = false,
    @argument(name = "file", description = "Input file")
    file: String = ""
)

Launcher.execute[MyApp]("--help")  // Shows help automatically
Launcher.execute[MyApp]("--verbose input.txt")  // Normal execution
```

#### LauncherConfig
```scala
case class LauncherConfig(
    helpPrinter: HelpPrinter = HelpPrinter.default,
    defaultCommand: LauncherInstance => Any = _ => (), // Show help by default
    helpPrefixes: Seq[String] = Seq("-h", "--help"),   // Customizable
    showHelpOnNoArgs: Boolean = true                    // Show help when no args
)
```

#### Supported Types
- `Boolean` → flag (no argument needed)
- `Option[T]` → optional value
- `Seq[T]`, `Array[T]` → multiple values
- `KeyValue` → single key=value pair (e.g., `-Lkey=value`)
- `Seq[KeyValue]` → multiple key=value pairs
- Primitives → required single value
- Nested case classes → flattened options

### Phase 7: Testing

**New file**: `uni/src/test/scala/wvlet/uni/cli/launcher/LauncherTest.scala`

Test cases:
- Built-in help option works by default
- Simple options and arguments
- Boolean flags
- Optional values
- Multiple values (arrays/lists)
- **Key-value pairs**: single and multiple
- Sub-commands via methods
- Nested options
- Help message generation
- Default commands
- Error handling (unknown options, missing required args)
- Custom help prefixes

## File Structure

```
uni/src/main/scala/wvlet/uni/
  surface/
    Surfaces.scala              # Modify: add annotations to ClassMethodSurface
    MethodSurface.scala         # Modify: add annotation helpers to trait
    CompileTimeSurfaceFactory.scala  # Modify: extract method annotations
  cli/
    launcher/
      annotations.scala         # New: @command, @option, @argument
      Launcher.scala           # New: entry point
      CommandLauncher.scala    # New: command wrapper
      OptionParser.scala       # New: CLI parsing
      OptionSchema.scala       # New: schema from annotations
      CLOption.scala           # New: option/argument items
      ParseTree.scala          # New: parsed data structure
      HelpPrinter.scala        # New: help generation
      CommandLineTokenizer.scala  # New: tokenization
      KeyValue.scala           # New: key=value pair type
uni/src/test/scala/wvlet/uni/cli/
  launcher/
    LauncherTest.scala         # New: comprehensive tests
    OptionParserTest.scala     # New: parser tests
    KeyValueTest.scala         # New: key-value parsing tests
```

## Cross-Platform Considerations

- **No JVM reflection**: All annotation reading at compile-time via Surface macros
- **Pure Scala data structures**: `ParseTree` replaces airframe's `StringTree`
- **Existing cross-platform utilities**: Uses `Chalk`, `CliEnv` for help output
- **Object instantiation**: Use Surface's `ObjectFactory` (already cross-platform)

## Verification

1. **Unit tests**: Run `sbt "uniJVM/testOnly *LauncherTest*"`
2. **Cross-platform compile**: `sbt uni/compile` (compiles all platforms)
3. **Cross-platform tests**: `sbt uniJS/test uniNative/test`
4. **Manual testing**: Create example CLI app and run with various arguments

## API Examples

### Simple Command (help is automatic)
```scala
case class MyApp(
    @option(prefix = "-v,--verbose", description = "Enable verbose output")
    verbose: Boolean = false,
    @argument(name = "file", description = "Input file")
    file: String = ""
)

// Help is built-in, no need to define it
Launcher.execute[MyApp]("--help")
// Output:
// usage: myapp [options] [file]
//
// [options]
//  -h, --help     Display this help message
//  -v, --verbose  Enable verbose output
//
// [arguments]
//  file           Input file
```

### Key-Value Options
```scala
case class LogConfig(
    @option(prefix = "-L", description = "Set log level (package=level)")
    logLevels: Seq[KeyValue] = Seq.empty,

    @option(prefix = "-D", description = "System property")
    props: Seq[KeyValue] = Seq.empty
)

// Command line: myapp -Lwvlet=debug -Lcom.example=info -Dfoo=bar
val config = Launcher.execute[LogConfig]("-Lwvlet=debug -Lcom.example=info -Dfoo=bar")
// config.logLevels == Seq(KeyValue("wvlet", "debug"), KeyValue("com.example", "info"))
// config.props == Seq(KeyValue("foo", "bar"))
```

### Sub-Commands
```scala
@command(description = "Git-like CLI tool")
class GitLike:
  @command(description = "Initialize repository")
  def init(@argument dir: String = "."): Unit = ???

  @command(description = "Clone repository")
  def clone(
      @argument url: String,
      @option(prefix = "--depth") depth: Option[Int] = None
  ): Unit = ???

// Help works at all levels
Launcher.execute[GitLike]("--help")        // Shows main help with sub-commands
Launcher.execute[GitLike]("clone --help")  // Shows clone command help
```

### Nested Options
```scala
case class GlobalOptions(
    @option(prefix = "-v,--verbose") verbose: Boolean = false,
    @option(prefix = "--config") configFile: Option[String] = None
)

case class MyCommand(
    global: GlobalOptions,  // Options flatten into parent
    @argument target: String = ""
)
```
