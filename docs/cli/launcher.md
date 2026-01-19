# Command Launcher

Build type-safe CLI applications with automatic argument parsing.

## Basic Usage

```scala
import wvlet.uni.cli.launcher.Launcher

case class MyApp(
  name: String = "world",
  count: Int = 1,
  verbose: Boolean = false
)

// Parse command-line arguments
val app = Launcher.execute[MyApp](args)
println(s"Hello, ${app.name}! (${app.count} times)")
```

Command line:
```bash
$ myapp --name Alice --count 3
Hello, Alice! (3 times)
```

## Argument Types

### String Arguments

```scala
case class App(
  name: String = "default"
)
```
```bash
$ app --name value
$ app -n value
```

### Numeric Arguments

```scala
case class App(
  count: Int = 1,
  timeout: Long = 1000
)
```
```bash
$ app --count 5 --timeout 5000
```

### Boolean Flags

```scala
case class App(
  verbose: Boolean = false,
  quiet: Boolean = false
)
```
```bash
$ app --verbose
$ app --quiet
$ app -v -q
```

### Optional Values

```scala
case class App(
  config: Option[String] = None
)
```
```bash
$ app --config path/to/config
$ app  # config is None
```

### Lists

```scala
case class App(
  files: List[String] = Nil
)
```
```bash
$ app --files a.txt --files b.txt
```

## Help Generation

Help is automatically generated:

```scala
case class MyApp(
  name: String = "world",
  count: Int = 1,
  verbose: Boolean = false
)

Launcher.of[MyApp].printHelp
```

Output:
```
Usage: myapp [options]

Options:
  --name <value>    (default: world)
  --count <value>   (default: 1)
  --verbose         (default: false)
  -h, --help        Show this help message
```

## Sub-Commands

Create applications with multiple commands:

```scala
case class Git():
  def clone(url: String): Unit =
    println(s"Cloning ${url}")

  def commit(message: String, all: Boolean = false): Unit =
    println(s"Committing: ${message}")

  def push(force: Boolean = false): Unit =
    println("Pushing...")

val launcher = Launcher.of[Git]
launcher.execute(args)
```

Command line:
```bash
$ git clone https://github.com/user/repo
$ git commit --message "Initial commit" --all
$ git push --force
```

## Adding Modules

Add sub-command modules:

```scala
case class MainApp()

case class DbCommands():
  def migrate(): Unit = ???
  def seed(): Unit = ???

case class UserCommands():
  def create(name: String): Unit = ???
  def delete(id: String): Unit = ???

val launcher = Launcher.of[MainApp]
  .addModule[DbCommands]("db", "Database commands")
  .addModule[UserCommands]("user", "User management")
```

Command line:
```bash
$ app db migrate
$ app db seed
$ app user create --name Alice
$ app user delete --id 123
```

## Configuration

```scala
import wvlet.uni.cli.launcher.{Launcher, LauncherConfig}

val launcher = Launcher.of[MyApp]
  .withHelpPrefixes(Seq("-h", "--help", "-?"))
  .withShowHelpOnNoArgs(true)
```

## Custom Help Printer

```scala
import wvlet.uni.cli.launcher.HelpPrinter

val customPrinter = new HelpPrinter:
  def printHelp(info: CommandInfo, schema: ClassOptionSchema): Unit =
    println(s"=== ${info.name} ===")
    println(info.description)
    // Custom formatting...

val launcher = Launcher.of[MyApp]
  .withHelpPrinter(customPrinter)
```

## Handling Results

```scala
val result = Launcher.of[MyApp].execute(args)

if result.showedHelp then
  // User requested help
  System.exit(0)
else
  // Use the parsed instance
  val app = result.instance.asInstanceOf[MyApp]
  app.run()
```

## Example: Complete CLI

```scala
import wvlet.uni.cli.launcher.Launcher
import wvlet.uni.cli.Chalk

case class FileTool(
  verbose: Boolean = false,
  output: Option[String] = None
):
  def copy(source: String, dest: String): Unit =
    if verbose then println(s"Copying ${source} to ${dest}")
    // Implementation...
    println(Chalk.green("Done!"))

  def move(source: String, dest: String): Unit =
    if verbose then println(s"Moving ${source} to ${dest}")
    // Implementation...
    println(Chalk.green("Done!"))

  def delete(path: String, force: Boolean = false): Unit =
    if verbose then println(s"Deleting ${path}")
    // Implementation...
    println(Chalk.green("Done!"))

object Main:
  def main(args: Array[String]): Unit =
    Launcher.execute[FileTool](args)
```

Usage:
```bash
$ filetool copy source.txt dest.txt --verbose
$ filetool move old.txt new.txt
$ filetool delete temp.txt --force
$ filetool --help
```

## Best Practices

1. **Provide defaults** for optional arguments
2. **Use descriptive names** for clarity
3. **Group related commands** in modules
4. **Test argument parsing** in unit tests
5. **Validate inputs** after parsing
