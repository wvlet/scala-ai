# Testing Framework

uni uses UniTest as its testing framework, providing a simple and expressive way to write tests across JVM, Scala.js, and Scala Native.

## Overview

| Component | Description |
|-----------|-------------|
| [UniTest](../core/unitest) | Full documentation (Core component) |
| [Assertions](./assertions) | Assertion quick reference |

## Quick Start

```scala
import wvlet.uni.test.UniTest

class MyServiceTest extends UniTest:
  test("basic assertion") {
    val result = 1 + 1
    result shouldBe 2
  }

  test("string matching") {
    val message = "Hello, World!"
    message shouldContain "World"
  }
```

## Running Tests

```bash
# Run all tests
sbt test

# Run specific module tests
sbt coreJVM/test

# Run specific test class
sbt "agent/testOnly *LLMAgentTest"

# With debug logging
sbt "coreJVM/testOnly * -- -l debug"
```

## Test Structure

### Test Class

```scala
import wvlet.uni.test.UniTest

class CalculatorTest extends UniTest:
  test("addition") {
    (2 + 2) shouldBe 4
  }

  test("subtraction") {
    (5 - 3) shouldBe 2
  }
```

### Nested Tests

```scala
class MathTest extends UniTest:
  test("arithmetic") {
    test("addition") {
      (1 + 1) shouldBe 2
    }

    test("multiplication") {
      (2 * 3) shouldBe 6
    }
  }
```

## Design Integration

Test with dependency injection:

```scala
import wvlet.uni.test.UniTest
import wvlet.uni.design.Design

class UserServiceTest extends UniTest:
  val testDesign = Design.newDesign
    .bindImpl[UserRepository, InMemoryUserRepository]
    .bindImpl[UserService, UserServiceImpl]

  test("create user") {
    testDesign.build[UserService] { service =>
      val user = service.createUser("Alice")
      user.name shouldBe "Alice"
    }
  }
```

## Best Practices

1. **Avoid mocks** - Use real implementations or in-memory versions
2. **Use Design** - Leverage DI for test isolation
3. **Descriptive names** - Clear test descriptions
4. **One assertion focus** - Each test verifies one behavior
5. **Clean state** - Tests should be independent

## Package

```scala
import wvlet.uni.test.UniTest
```
