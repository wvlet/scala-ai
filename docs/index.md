---
layout: home

hero:
  name: wvlet/uni
  text: Scala 3 Unified Utility Library
  tagline: Essential utilities for building robust Scala applications - logging, DI, JSON, HTTP, reactive streams, and LLM agent framework
  actions:
    - theme: brand
      text: Get Started
      link: /guide/
    - theme: alt
      text: View on GitHub
      link: https://github.com/wvlet/uni

features:
  - title: Dependency Injection
    details: Compile-time safe dependency injection with lifecycle management and session-scoped bindings.
  - title: Logging & Diagnostics
    details: Structured logging with LogSupport trait, configurable log levels, and source code location tracking.
  - title: JSON & MessagePack
    details: Pure Scala JSON parser with DSL support and efficient MessagePack binary serialization.
  - title: HTTP Client
    details: Cross-platform HTTP client with automatic retry, circuit breaker integration, and streaming support.
  - title: Reactive Streams
    details: Rx-based reactive programming with operators for mapping, filtering, combining, and throttling streams.
  - title: LLM Agent Framework
    details: Build AI agents with tool calling, chat sessions, and AWS Bedrock integration.
---

## Quick Example

```scala
import wvlet.uni.design.Design
import wvlet.uni.log.LogSupport
import wvlet.uni.http.Http

// Define a service with logging
class MyService extends LogSupport:
  def greet(name: String): String =
    info(s"Greeting ${name}")
    s"Hello, ${name}!"

// Use dependency injection
val design = Design.newDesign
  .bindSingleton[MyService]

design.build[MyService] { service =>
  println(service.greet("World"))
}
```

## Modules

| Module | Description |
|--------|-------------|
| `uni` | Core utilities: DI, logging, JSON, MessagePack, Rx, HTTP client |
| `uni-agent` | LLM agent framework with tool integration |
| `uni-agent-bedrock` | AWS Bedrock chat model integration |

## Cross-Platform Support

uni is designed for cross-platform Scala development:

- **JVM** - Full feature support
- **Scala.js** - Browser and Node.js environments
- **Scala Native** - Native binary compilation
