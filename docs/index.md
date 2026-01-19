---
layout: home

hero:
  name: Uni
  text: Essential Scala Utilities
  tagline: Refined for Scala 3 with minimal dependencies
  actions:
    - theme: brand
      text: Get Started
      link: /guide/
    - theme: alt
      text: View on GitHub
      link: https://github.com/wvlet/uni

features:
  - title: Core Primitives
    details: Logging, Design (object wiring), Rx (async programming), and unified serialization through JSON and MessagePack.
    link: /core/
  - title: HTTP Client & Server
    details: Full-featured HTTP with automatic retry, circuit breaker, rate limiting, and streaming support.
    link: /http/
  - title: CLI Utilities
    details: Terminal styling, progress indicators, and type-safe command-line argument parsing.
    link: /cli/
  - title: LLM Agent Framework
    details: Build AI agents with tool calling, chat sessions, and AWS Bedrock integration.
    link: /agent/
---

## Getting Started

::: code-group

```scala [sbt]
libraryDependencies += "org.wvlet" %% "uni" % "2025.1.0"
```

```scala [Scala CLI]
//> using dep org.wvlet::uni:2025.1.0
```

:::

## Quick Example

```scala
import wvlet.uni.log.LogSupport

class MyService extends LogSupport:
  def greet(name: String): String =
    info(s"Greeting ${name}")
    s"Hello, ${name}!"

@main def hello =
  val service = MyService()
  println(service.greet("World"))
```

## Modules

| Module | Description |
|--------|-------------|
| `uni` | Core utilities: Design, logging, JSON, MessagePack, Rx, HTTP client |
| `uni-agent` | LLM agent framework with tool integration |
| `uni-agent-bedrock` | AWS Bedrock chat model integration |

## Cross-Platform Support

uni is designed for cross-platform Scala development:

- **JVM** - Full feature support
- **Scala.js** - Browser and Node.js environments
- **Scala Native** - Native binary compilation
