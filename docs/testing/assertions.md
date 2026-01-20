# Assertions & Matchers

UniTest provides expressive assertions for testing. See the [full UniTest documentation](../core/unitest) for comprehensive coverage.

## Basic Assertions

### Equality

```scala
result shouldBe expected
result shouldNotBe unexpected
```

### Boolean

```scala
condition shouldBe true
condition shouldBe false
```

### Null Checks

```scala
value shouldBe null
value shouldNotBe null
```

## Collection Assertions

### Contains

```scala
list shouldContain element
string shouldContain substring
```

### Size

```scala
list.size shouldBe 3
list.isEmpty shouldBe false
```

## Comparison Assertions

Use boolean expressions with `shouldBe true`:

```scala
// Greater than
(value > 10) shouldBe true

// Less than
(value < 100) shouldBe true

// Greater than or equal
(value >= 0) shouldBe true

// Range check
(value >= 0 && value <= 100) shouldBe true
```

## Type Assertions

### Pattern Matching (Preferred)

```scala
result shouldMatch { case x: ExpectedType => }

// With value extraction
result shouldMatch {
  case User(name, age) =>
    name shouldBe "Alice"
    age shouldBe 30
}
```

### Avoid asInstanceOf

```scala
// Bad - can throw ClassCastException
val user = result.asInstanceOf[User]

// Good - type-safe pattern matching
result shouldMatch { case user: User =>
  user.name shouldBe "Alice"
}
```

## Exception Assertions

### Expect Exception

```scala
intercept[IllegalArgumentException] {
  functionThatThrows()
}
```

### With Message Check

```scala
val e = intercept[RuntimeException] {
  failingOperation()
}
e.getMessage shouldContain "expected error"
```

## Option Assertions

```scala
option shouldBe Some(expected)
option shouldBe None

option.isDefined shouldBe true
option.isEmpty shouldBe true
```

## String Assertions

```scala
str shouldBe "expected"
str shouldContain "substring"
str.startsWith("prefix") shouldBe true
str.endsWith("suffix") shouldBe true
str.matches("regex.*") shouldBe true
```

## Examples

### Testing a Calculator

```scala
class CalculatorTest extends UniTest:

  val calc = Calculator()

  test("add two numbers") {
    calc.add(2, 3) shouldBe 5
  }

  test("divide by zero throws exception") {
    intercept[ArithmeticException] {
      calc.divide(10, 0)
    }
  }

  test("result is positive") {
    val result = calc.add(1, 1)
    (result > 0) shouldBe true
  }
```

### Testing a User Service

```scala
class UserServiceTest extends UniTest:

  val service = UserService()

  test("create user returns User type") {
    val result = service.create("Alice", "alice@example.com")

    result shouldMatch { case user: User =>
      user.name shouldBe "Alice"
      user.email shouldBe "alice@example.com"
    }
  }

  test("find user returns Some for existing user") {
    service.create("Bob", "bob@example.com")
    val found = service.findByName("Bob")

    found.isDefined shouldBe true
    found.get.name shouldBe "Bob"
  }

  test("find user returns None for missing user") {
    val found = service.findByName("NonExistent")
    found shouldBe None
  }
```

### Testing Collections

```scala
class CollectionTest extends UniTest:

  test("list operations") {
    val list = List(1, 2, 3, 4, 5)

    list.size shouldBe 5
    list shouldContain 3
    list.head shouldBe 1
    list.last shouldBe 5
  }

  test("map operations") {
    val map = Map("a" -> 1, "b" -> 2)

    map.size shouldBe 2
    map.contains("a") shouldBe true
    map("a") shouldBe 1
  }
```

### Testing with Design

```scala
class IntegrationTest extends UniTest:

  val testDesign = Design.newDesign
    .bindImpl[Database, InMemoryDatabase]
    .bindImpl[UserRepository, UserRepositoryImpl]

  test("repository stores and retrieves users") {
    testDesign.build[UserRepository] { repo =>
      val user = User("123", "Alice")
      repo.save(user)

      val found = repo.findById("123")
      found shouldBe Some(user)
    }
  }
```

## Best Practices

1. **Use `shouldMatch`** for type checking instead of `asInstanceOf`
2. **Prefer `shouldBe`** for simple equality
3. **Use boolean expressions** for comparisons: `(x > 5) shouldBe true`
4. **One assertion per test** when possible
5. **Meaningful test names** that describe the behavior
