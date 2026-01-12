# CLAUDE.md

## Project Overview

**wvlet/uni** is a Scala 3 unified utility library:

- **uni-core**: Foundational utilities from wvlet/airframe (logging, DI, JSON/MessagePack, RPC/HTTP, testing)
- **uni-agent**: Agent interfaces, orchestration, and tool integration
- **uni-agent-bedrock**: AWS Bedrock integration

## Architecture

- `LLMAgent`: Core agent abstraction
- `ChatModel`: Provider interface (e.g., `BedrockChat`)
- `ChatSession`: Conversation state
- `ToolSpec`: Function calling
- Message types: `UserMessage`, `AIMessage`, `SystemMessage`, `ToolResultMessage`, `AIReasoningMessage`

Cross-platform: JVM, Scala.js, Scala Native via sbt-crossproject. Platform-specific code in `.jvm`, `.js`, `.native` folders.

## Commands

```bash
sbt compile                              # Compile all
sbt test                                 # Test all
sbt coreJVM/test                         # Test specific module
sbt "agent/testOnly *LLMAgentTest"       # Test specific class
sbt "coreJVM/testOnly * -- -l debug"     # With debug logging
sbt scalafmtAll                          # Format (CI checks this)
sbt integrationTest/test                 # Integration tests (requires AWS creds)
```

## Testing (AirSpec)

Avoid mocks. Use `shouldBe`, `shouldNotBe`, `shouldContain`, `shouldMatch`.

```scala
// Comparison operators
(value >= 1) shouldBe true  // NOT: should be >= 1

// Type checking
result shouldMatch { case x: ExpectedType => }  // NOT: .asInstanceOf[X]
```

See `.github/instructions/airspec.instructions.md` for more.

## Coding Style

- Scala 3 syntax only
- Omit `new`: `StringBuilder()` not `new StringBuilder()`
- String interpolation: always use `${...}` with brackets
- Avoid `Try[A]` return types
- Config classes: `withXXX(...)` for all fields, `noXXX()` for optional fields
- uni-core: minimal dependencies only

## Git Workflow

### Branches and PRs
- Never push directly to main. All changes require PRs.
- Create branch FIRST: `git switch -c <prefix>/<description>`
- Prefixes: `feature/`, `fix/`, `chore/`, `deps/`, `docs/`, `test/`, `breaking/`
- Use `gh` for PR management
- Never enable auto-merge without user approval

### Commits
- Prefixes: `feature`, `fix`, `internal`, `doc`
- Focus on "why" not "what"
- Example: `feature: Add XXX to improve user experience`

### Code Reviews
Gemini reviews PRs. Address feedback before merging.
