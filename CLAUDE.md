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
- Mock implementations available for testing without external dependencies

### AirSpec Assertion Syntax
- Use `shouldBe`, `should include()`, `shouldBe a[Type]` for basic assertions
- For comparison operators, wrap in parentheses: `(value >= 1) shouldBe true`
- Avoid ScalaTest-style matchers like `should be >= 1` - not supported in AirSpec

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

## Important Implementation Notes

- BedrockChat implements streaming responses using AWS SDK's ConverseStream API
- Tool calling uses JSON Schema for parameter validation
- Reasoning support for models like Claude-3.5-Sonnet with thinking capabilities
- Circuit breaker and retry logic available in ai-core for resilience
- Dependency injection design allows for easy testing and configuration