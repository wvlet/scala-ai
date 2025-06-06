# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**wvlet/scala-ai** is a Scala 3 library for building AI-driven applications and agentic systems. It consists of two main components:

- **ai-core**: Foundational utilities adapted from wvlet/airframe, including logging, dependency injection, serialization (JSON/MessagePack), RPC/HTTP framework, and testing
- **ai-agent**: High-level AI agent interfaces providing unified LLM APIs, agent orchestration, and tool integration
- **ai-agent-bedrock**: AWS Bedrock integration for the ai-agent module

## Key Architecture Concepts

### LLM Agent Architecture
- `LLMAgent` is the core abstraction for AI agents with identity, model, system prompt, tools, and configuration
- `ChatModel` trait defines the interface for LLM providers (e.g., BedrockChat)
- `ChatSession` manages conversation history and state
- Tool integration through `ToolSpec` for function calling capabilities

### Multi-Platform Support
- Core library supports JVM, Scala.js, and Scala Native
- Cross-platform build using sbt-crossproject
- Platform-specific dependencies handled in build configuration

### Message System
- Unified message types: `UserMessage`, `AIMessage`, `SystemMessage`, `ToolResultMessage`
- Support for reasoning messages (`AIReasoningMessage`) for o1-style models
- Chat streaming with observer pattern for real-time responses

## Common Development Commands

### Build and Test
```bash
# Compile all modules
sbt compile

# Run all tests
sbt test

# Run tests for specific module
sbt coreJVM/test
sbt agent/test
sbt bedrock/test

# Run a specific test class
sbt "agent/testOnly *LLMAgentTest"
sbt "coreJVM/testOnly *StringWeaverTest"

# Run with debug log enabled
sbt "coreJVM/testOnly * -- -l debug"

# Cross-platform builds
sbt projectJVM/compile    # JVM only
sbt projectJS/compile     # Scala.js only
sbt projectNative/compile # Scala Native only
```

### Code Formatting

Ensure the code is formatted with `scalafmtAll` command for consistent code style. CI will check formatting on pull requests.

```bash
# Format code
sbt scalafmtAll

# Check formatting
sbt scalafmtCheck
```

### Integration Tests
```bash
# Run integration tests (requires AWS credentials)
sbt integrationTest/test
```

## Testing Framework

- Uses AirSpec testing framework (lightweight alternative to ScalaTest)
- Test files end with `Test.scala` or `Spec.scala`
- Integration tests are in separate `ai-integration-test` module
- Avoid using mock as it increases maintenance cost and creates brittle tests that break when internal implementation changes
- Ensure tests cover new functionality and bug fixes with good test coverage
- Test names should be concise and descriptive, written in plain English
  - Good: `"should parse JSON with nested objects"`, `"should handle connection timeout gracefully"`
  - Avoid: `"testParseJSON"`, `"test1"`, `"shouldWork"`

### AirSpec Assertion Syntax
- Use `shouldBe`, `shouldNotBe`, `shouldContain`, `shouldMatch` for basic assertions
- For type check, use `shouldMatch { case x:X => ... }` syntax.
- For comparison operators, use `(cond) shouldBe true`: `(value >= 1) shouldBe true`
- Avoid ScalaTest-style matchers like `should be >= 1` - not supported in AirSpec
- For more syntax examples, refer to .github/instructions/airspec.instructions.md

## Key Dependencies

- **Scala Version**: 3.7.0
- **Airframe**: 2025.1.12 (logging, DI, serialization)
- **AWS SDK**: 2.31.55 (Bedrock integration)
- **Testing**: AirSpec framework

## Module Structure

- `ai-core/`: Cross-platform core utilities (logging, DI, JSON, MessagePack, etc.)
- `ai-agent/`: Core LLM agent interfaces and chat abstractions
- `ai-agent-bedrock/`: AWS Bedrock-specific implementation
- `ai-integration-test/`: Integration tests requiring real LLM services

## Coding Style Guidelines

- Use Scala 3 syntax throughout the codebase (no Scala 2 support needed)
- For ai-core cross-platform development, use .jvm, .js, and .native folders for platform-specific code
- Omit `new` for object instantiation (e.g., `StringBuilder()` instead of `new StringBuilder()`)
- Always enclose expressions in string interpolation with brackets: `${...}`
- Document public APIs (classes, methods, objects) with [Scaladoc comments](https://docs.scala-lang.org/style/scaladoc.html)
- Avoid returning Try[A] as it forces monadic-style usage
- Configuration case classes should have `withXXX(...)` methods for all fields and `noXXX(...)` methods for optional fields
  - Example: `case class Config(host: String, port: Int, timeout: Option[Duration])` should have:
    - `def withHost(host: String): Config = copy(host = host)`
    - `def withPort(port: Int): Config = copy(port = port)`
    - `def withTimeout(timeout: Duration): Config = copy(timeout = Some(timeout))`
    - `def noTimeout(): Config = copy(timeout = None)`

## Git and Development Workflow

### Branching
- Create new branches with timestamp: `git switch -c feature/$(date +"%Y%m%d_%H%M%S")`
- Use appropriate prefixes: `feature/`, `fix/`, `doc/`, `internal/`
- Optionally add brief description: `fix/$(date +"%Y%m%d_%H%M%S")-correct-off-by-one-error`

### Commit Messages
- Use prefixes: `feature` (new features), `fix` (bug fixes), `internal` (non-user facing), `doc` (documentation)
- Focus on "why" rather than "what" or "how"
- Good example: `feature: Add XXX to improve user experience`
- Avoid: `feature: Add XXX class`

### Pull Requests
- Use [`gh pr create`](https://cli.github.com/manual/gh_pr_create) with clear title and detailed body
- Follow .github/pull_request_template.md format
- Merge with squash via `gh pr merge --squash --auto` for clean history

## Important Implementation Notes

- BedrockChat implements streaming responses using AWS SDK's ConverseStream API
- Tool calling uses JSON Schema for parameter validation
- Reasoning support for models like Claude-3.5-Sonnet with thinking capabilities
- Circuit breaker and retry logic available in ai-core for resilience
- Dependency injection design allows for easy testing and configuration
- ai-core designed with minimal dependencies - avoid libraries not in core module