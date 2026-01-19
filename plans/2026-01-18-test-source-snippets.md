# Plan: Source Code Snippets for Test Assertion Failures

## Goal

Enhance uni-test to show source code snippets when assertions fail, making it easier to identify what failed.

**Example output:**
```
  - my test: Expected <5> but got <3> (MyTest.scala:42)

    42| value shouldBe 5
        ^^^^^
```

## Design Summary

**Approach:** Compile-time source line capture (cross-platform)

Capture the source line text at compile time using Scala 3 macros. This works on all platforms (JVM, JS, Native) with no runtime file I/O.

## Implementation Steps

### 1. Enhance TestSource

**File:** `uni-test/src/main/scala/wvlet/uni/test/TestSource.scala`

- Add `sourceLine: String` field to capture the actual code at compile time
- Enhance `generateImpl` macro to read source content from `pos.sourceFile.content`
- Add `formatSnippet` method for display

```scala
case class TestSource(fileName: String, line: Int, col: Int, sourceLine: String = ""):
  def fileLocation = s"${fileName}:${line}"

  def formatSnippet: Option[String] =
    if sourceLine.isEmpty then None
    else
      val linePrefix = s"${line}| "
      val caretLine = if col > 0 then " " * (linePrefix.length + col - 1) + "^" else ""
      Some(s"${linePrefix}${sourceLine}\n${caretLine}".stripTrailing())
```

### 2. Update UniTestTask to display snippets

**File:** `uni-test/src/main/scala/wvlet/uni/test/spi/UniTestTask.scala`

Modify `logResult()` to show snippet on failure:

```scala
case TestResult.Failure(name, msg, cause) =>
  loggers.foreach(_.error(s"${RED}  - ${name}: ${msg}${RESET}"))
  cause.foreach {
    case af: AssertionFailure =>
      af.source.formatSnippet.foreach { s =>
        loggers.foreach(_.error(s"${GRAY}    ${s.replace("\n", "\n    ")}${RESET}"))
      }
    case _ => ()
  }
```

## Files to Modify

| File | Change |
|------|--------|
| `uni-test/src/main/scala/wvlet/uni/test/TestSource.scala` | Add `sourceLine` field, enhance macro, add `formatSnippet` |
| `uni-test/src/main/scala/wvlet/uni/test/spi/UniTestTask.scala` | Display snippet in `logResult()` |

## Verification

1. Run `sbt uniTestJVM/test` - verify snippets appear on failures
2. Run `sbt uniTestJS/test` - verify snippets work on JS
3. Run `sbt uniTestNative/test` - verify snippets work on Native
4. Check output format matches example above
