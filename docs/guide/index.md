# Introduction

**wvlet/uni** is a Scala 3 unified utility library that provides essential building blocks for robust application development. It consolidates foundational utilities from the [wvlet/airframe](https://github.com/wvlet/airframe) project into a single, cohesive library.

## What is uni?

uni provides:

- **Dependency Injection** - Compile-time safe DI with lifecycle management
- **Logging** - Structured logging with source code location tracking
- **Serialization** - JSON parsing/generation and MessagePack binary format
- **HTTP Client** - Cross-platform client with retry and streaming support
- **Reactive Streams** - Rx-based operators for async data flows
- **Control Flow** - Retry logic, circuit breakers, and resource management
- **CLI Utilities** - Terminal styling, progress bars, and command launching
- **Type Introspection** - Compile-time reflection with Surface

## Design Philosophy

uni follows these principles:

1. **Minimal Dependencies** - Core functionality without heavy external dependencies
2. **Cross-Platform** - Works on JVM, Scala.js, and Scala Native
3. **Scala 3 First** - Modern Scala 3 syntax and features
4. **Composable** - Small, focused utilities that combine well
5. **Production Ready** - Battle-tested code from the Airframe ecosystem

## Module Structure

```
uni/                    # Core utilities
├── design/            # Dependency injection
├── log/               # Logging
├── json/              # JSON processing
├── msgpack/           # MessagePack serialization
├── rx/                # Reactive streams
├── http/              # HTTP client
├── control/           # Retry, circuit breaker, caching
├── cli/               # Terminal utilities
├── surface/           # Type introspection
└── weaver/            # Object serialization

uni-agent/             # LLM agent framework
├── chat/              # Chat sessions and messages
├── tool/              # Tool execution
└── runner/            # Agent orchestration

uni-agent-bedrock/     # AWS Bedrock integration
```

## Next Steps

- [Installation](./installation) - Add uni to your project
- [Design Principles](./principles) - Learn about the architecture
- [Core Utilities](/core/) - Explore the foundational APIs
