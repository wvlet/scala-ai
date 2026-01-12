package example

import wvlet.uni.test.UniTest
import wvlet.uni.test.empty
import wvlet.uni.test.defined

// Example data classes for testing
case class User(id: Int, name: String, email: Option[String] = None):
  def withEmail(email: String): User = this.copy(email = Some(email))
  def noEmail: User                  = this.copy(email = None)

case class UserService(users: List[User]):
  def findById(id: Int): Option[User]      = users.find(_.id == id)
  def findByName(name: String): List[User] = users.filter(_.name.contains(name))
  def addUser(user: User): UserService     = UserService(users :+ user)

/**
  * Example AirSpec test class demonstrating basic testing patterns
  */
class AirSpecTest extends UniTest:

  test("basic assertions with shouldBe") {
    val user = User(1, "Alice")

    // Basic equality checks
    user.id shouldBe 1
    user.name shouldBe "Alice"
    user.email shouldBe None

    // Checking object equality
    val sameUser = User(1, "Alice")
    user shouldBe sameUser

    // Different users should not be equal
    val differentUser = User(2, "Bob")
    user shouldNotBe differentUser
  }

  test("collection assertions") {
    val users = List(User(1, "Alice"), User(2, "Bob"), User(3, "Charlie"))

    // Collection size and emptiness
    users shouldNotBe empty
    users.size shouldBe 3
    List.empty[User] shouldBe empty

    // Collection containment
    users shouldContain User(1, "Alice")
    users shouldNotContain User(4, "David")

    // Collection equality
    val sameUsers = List(User(1, "Alice"), User(2, "Bob"), User(3, "Charlie"))
    users shouldBe sameUsers
  }

  test("option handling") {
    val userWithEmail    = User(1, "Alice", Some("alice@example.com"))
    val userWithoutEmail = User(2, "Bob")

    // Option checks
    userWithEmail.email shouldBe defined
    userWithEmail.email shouldNotBe empty
    userWithEmail.email shouldBe Some("alice@example.com")

    userWithoutEmail.email shouldNotBe defined
    userWithoutEmail.email shouldBe empty
    userWithoutEmail.email shouldBe None
  }

  test("string operations") {
    val message = "Hello, AirSpec Testing!"

    // String containment
    message shouldContain "AirSpec"
    message shouldContain "Testing"
    message shouldNotContain "ScalaTest"

    // String properties
    message.length shouldBe 23
    message.toLowerCase shouldContain "hello"
  }

  test("pattern matching with shouldMatch") {
    val users = List(User(1, "Alice"), User(2, "Bob"))

    users shouldMatch {
      case List(User(1, "Alice", None), User(2, "Bob", None)) => // matches
      case _                                                  =>
        fail("Pattern should match")
    }

    val user = User(1, "Alice", Some("alice@example.com"))
    user shouldMatch {
      case User(_, "Alice", Some(_)) => // matches
      case _                         =>
        fail("User pattern should match")
    }
  }

  test("exception handling") {
    // Testing expected exceptions
    intercept[IllegalArgumentException] {
      throw IllegalArgumentException("Invalid input")
    }

    // Testing that no exception is thrown
    val result = User(1, "Alice").withEmail("alice@example.com")
    result.email shouldBe Some("alice@example.com")
  }

  test("object instance equality") {
    val user1    = User(1, "Alice")
    val user1Ref = user1
    val user2    = User(1, "Alice") // same content, different instance

    // Content equality
    user1 shouldBe user2

    // Instance equality
    user1 shouldBeTheSameInstanceAs user1Ref
    user1 shouldNotBeTheSameInstanceAs user2
  }

  test("float and double comparisons") {
    val pi            = 3.14159
    val approximatePi = 3.14

    // Exact equality
    pi shouldBe 3.14159

    // Approximate equality with delta
    assertEquals(pi, approximatePi, 0.01)
    assertEquals(pi, 3.14, 0.01)
  }

  test("working with UserService") {
    val users = List(
      User(1, "Alice Johnson", Some("alice@example.com")),
      User(2, "Bob Smith"),
      User(3, "Alice Brown", Some("alice.brown@example.com"))
    )
    val service = UserService(users)

    test("finding users by ID") {
      val found = service.findById(1)
      found shouldBe defined
      found.get.name shouldBe "Alice Johnson"

      val notFound = service.findById(999)
      notFound shouldNotBe defined
    }

    test("finding users by name") {
      val aliceUsers = service.findByName("Alice")
      aliceUsers.size shouldBe 2
      aliceUsers shouldContain User(1, "Alice Johnson", Some("alice@example.com"))
      aliceUsers shouldContain User(3, "Alice Brown", Some("alice.brown@example.com"))

      val bobUsers = service.findByName("Bob")
      bobUsers.size shouldBe 1
      bobUsers.head.name shouldBe "Bob Smith"
    }

    test("adding new users") {
      val newUser        = User(4, "David Wilson")
      val updatedService = service.addUser(newUser)

      updatedService.users.size shouldBe 4
      updatedService.findById(4) shouldBe Some(newUser)

      // Original service should be unchanged
      service.users.size shouldBe 3
    }
  }

  test("testing case class with methods") {
    val user = User(1, "Alice")

    // Testing withXXX methods
    val userWithEmail = user.withEmail("alice@example.com")
    userWithEmail.email shouldBe Some("alice@example.com")
    userWithEmail.id shouldBe 1
    userWithEmail.name shouldBe "Alice"

    // Testing noXXX methods
    val userNoEmail = userWithEmail.noEmail
    userNoEmail.email shouldBe None
    userNoEmail.id shouldBe 1
    userNoEmail.name shouldBe "Alice"
  }

  test("debug and trace logging example") {
    val user = User(1, "Alice")

    debug(s"Testing user: ${user}")
    trace(s"User details - ID: ${user.id}, Name: ${user.name}, Email: ${user.email}")

    user.name shouldBe "Alice"

    debug("Test completed successfully")
  }

  test("demonstrating test control flow") {
    test("this test will pass") {
      1 + 1 shouldBe 2
    }

    // Uncomment to see different test states:
    // ignore("this test is ignored") {
    //   fail("This should not run")
    // }

    // pending("this test is pending") {
    //   // Implementation pending
    // }

    // skip("this test is skipped") {
    //   // Not applicable for current environment
    // }
  }

end AirSpecTest
