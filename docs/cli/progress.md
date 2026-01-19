# Progress Indicators

Display progress bars and spinners for long-running operations.

## Progress Bar

### Basic Usage

```scala
import wvlet.uni.cli.ProgressBar

val bar = ProgressBar(total = 100).start()

for i <- 1 to 100 do
  Thread.sleep(50)
  bar.increment()

bar.finish()
```

### Custom Styling

```scala
import wvlet.uni.cli.{ProgressBar, Chalk}

val bar = ProgressBar(total = 1000)
  .withWidth(30)                    // Bar width
  .withPrefix("Downloading: ")      // Text before bar
  .withSuffix(" files")             // Text after bar
  .withShowPercent(true)            // Show percentage
  .withShowCount(true)              // Show count (e.g., 50/100)
  .withColor(Chalk.cyan)            // Bar color
  .start()
```

### Update Progress

```scala
val bar = ProgressBar(total = 100).start()

// Set to specific value
bar.update(50)

// Increment by 1
bar.increment()

// Increment by specific amount
bar.increment(10)

// Check if complete
if bar.isComplete then
  bar.finish()
```

### Success and Failure

```scala
val bar = ProgressBar(total = 100).start()

try
  for i <- 1 to 100 do
    bar.increment()
    riskyOperation(i)
  bar.finish()  // Success
catch
  case e: Exception =>
    bar.fail()  // Failure
    throw e
```

## Configuration Options

| Option | Description | Default |
|--------|-------------|---------|
| `total` | Total items to process | Required |
| `width` | Width of the bar in characters | 40 |
| `complete` | Character for completed portion | █ |
| `incomplete` | Character for incomplete portion | ░ |
| `prefix` | Text before the bar | "" |
| `suffix` | Text after the bar | "" |
| `showPercent` | Show percentage | true |
| `showCount` | Show item count | false |
| `color` | Bar color | Chalk.green |
| `hideCursor` | Hide cursor during progress | true |

## Spinner

For indeterminate operations:

```scala
import wvlet.uni.cli.Spinner

val spinner = Spinner()
  .withMessage("Loading...")
  .start()

// Do work...
Thread.sleep(3000)

spinner.stop()
```

### Spinner Styles

```scala
import wvlet.uni.cli.{Spinner, SpinnerStyle}

// Different animation styles
Spinner().withStyle(SpinnerStyle.Dots)
Spinner().withStyle(SpinnerStyle.Line)
Spinner().withStyle(SpinnerStyle.Arrow)
```

## Disabling Progress

For non-interactive environments:

```scala
val isInteractive = System.console() != null

val bar = ProgressBar(total = 100)
  .withEnabled(isInteractive)
  .start()

// Works the same, but no output if disabled
bar.increment()
bar.finish()
```

## Example: File Processing

```scala
import wvlet.uni.cli.{ProgressBar, Chalk}
import java.io.File

def processFiles(files: List[File]): Unit =
  val bar = ProgressBar(total = files.size)
    .withPrefix("Processing: ")
    .withShowCount(true)
    .withColor(Chalk.blue)
    .start()

  for file <- files do
    processFile(file)
    bar.increment()

  bar.finish()
  println(Chalk.green("All files processed!"))
```

## Example: Download Progress

```scala
def downloadWithProgress(url: String, totalBytes: Long): Unit =
  val bar = ProgressBar(total = totalBytes)
    .withPrefix("Downloading: ")
    .withWidth(50)
    .start()

  var downloaded = 0L
  while downloaded < totalBytes do
    val chunk = downloadChunk(url, downloaded)
    downloaded += chunk.length
    bar.update(downloaded)

  bar.finish()
```

## Best Practices

1. **Set realistic totals** - Know the work upfront
2. **Update frequently enough** - But not too often
3. **Handle interruption** - Call finish() or fail()
4. **Disable in CI** - Check for TTY
5. **Use spinners** for unknown duration
