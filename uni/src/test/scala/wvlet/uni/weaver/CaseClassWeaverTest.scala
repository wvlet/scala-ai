package wvlet.uni.weaver

import wvlet.uni.test.UniTest

// Test case classes defined outside the test class
case class Person(name: String, age: Int) derives ObjectWeaver

case class Address(city: String, country: String) derives ObjectWeaver

case class Employee(name: String, address: Address) derives ObjectWeaver

case class Profile(name: String, email: Option[String] = None) derives ObjectWeaver

case class Team(name: String, members: List[Person]) derives ObjectWeaver

case class User(firstName: String, lastName: String) derives ObjectWeaver

case class ConfigWithDefaults(name: String, timeout: Int = 30, retries: Int = 3)
    derives ObjectWeaver

class CaseClassWeaverTest extends UniTest:

  test("simple case class round-trip") {
    val person   = Person("Alice", 30)
    val msgpack  = ObjectWeaver.weave(person)
    val restored = ObjectWeaver.unweave[Person](msgpack)
    restored shouldBe person
  }

  test("simple case class to/from JSON") {
    val person   = Person("Bob", 25)
    val json     = ObjectWeaver.toJson(person)
    val restored = ObjectWeaver.fromJson[Person](json)
    restored shouldBe person
  }

  test("nested case classes") {
    val employee = Employee("Charlie", Address("Tokyo", "Japan"))
    val msgpack  = ObjectWeaver.weave(employee)
    val restored = ObjectWeaver.unweave[Employee](msgpack)
    restored shouldBe employee
  }

  test("nested case classes to/from JSON") {
    val employee = Employee("Diana", Address("New York", "USA"))
    val json     = ObjectWeaver.toJson(employee)
    val restored = ObjectWeaver.fromJson[Employee](json)
    restored shouldBe employee
  }

  test("optional fields with Some value") {
    val profile  = Profile("Eve", Some("eve@example.com"))
    val msgpack  = ObjectWeaver.weave(profile)
    val restored = ObjectWeaver.unweave[Profile](msgpack)
    restored shouldBe profile
  }

  test("optional fields with None") {
    val profile  = Profile("Frank", None)
    val msgpack  = ObjectWeaver.weave(profile)
    val restored = ObjectWeaver.unweave[Profile](msgpack)
    restored shouldBe profile
  }

  test("optional fields missing in input") {
    // JSON without the optional field should use default None
    val json     = """{"name":"Grace"}"""
    val restored = ObjectWeaver.fromJson[Profile](json)
    restored shouldBe Profile("Grace", None)
  }

  test("collections of case classes") {
    val team     = Team("Engineers", List(Person("Alice", 30), Person("Bob", 25)))
    val msgpack  = ObjectWeaver.weave(team)
    val restored = ObjectWeaver.unweave[Team](msgpack)
    restored shouldBe team
  }

  test("collections of case classes to/from JSON") {
    val team     = Team("Designers", List(Person("Carol", 28), Person("Dave", 32)))
    val json     = ObjectWeaver.toJson(team)
    val restored = ObjectWeaver.fromJson[Team](json)
    restored shouldBe team
  }

  test("empty collection") {
    val team     = Team("Empty", List.empty)
    val msgpack  = ObjectWeaver.weave(team)
    val restored = ObjectWeaver.unweave[Team](msgpack)
    restored shouldBe team
  }

  test("default values for missing fields") {
    val json     = """{"name":"MyConfig"}"""
    val restored = ObjectWeaver.fromJson[ConfigWithDefaults](json)
    restored shouldBe ConfigWithDefaults("MyConfig", 30, 3)
  }

  test("default values can be overridden") {
    val json     = """{"name":"MyConfig","timeout":60,"retries":5}"""
    val restored = ObjectWeaver.fromJson[ConfigWithDefaults](json)
    restored shouldBe ConfigWithDefaults("MyConfig", 60, 5)
  }

  test("partial default values") {
    val json     = """{"name":"MyConfig","timeout":120}"""
    val restored = ObjectWeaver.fromJson[ConfigWithDefaults](json)
    restored shouldBe ConfigWithDefaults("MyConfig", 120, 3)
  }

  test("canonicalized name matching - snake_case") {
    val json     = """{"first_name":"Alice","last_name":"Smith"}"""
    val restored = ObjectWeaver.fromJson[User](json)
    restored shouldBe User("Alice", "Smith")
  }

  test("canonicalized name matching - kebab-case") {
    val json     = """{"first-name":"Bob","last-name":"Jones"}"""
    val restored = ObjectWeaver.fromJson[User](json)
    restored shouldBe User("Bob", "Jones")
  }

  test("canonicalized name matching - UPPER_SNAKE_CASE") {
    val json     = """{"FIRST_NAME":"Charlie","LAST_NAME":"Brown"}"""
    val restored = ObjectWeaver.fromJson[User](json)
    restored shouldBe User("Charlie", "Brown")
  }

  test("canonicalized name matching - camelCase") {
    val json     = """{"firstName":"Diana","lastName":"Prince"}"""
    val restored = ObjectWeaver.fromJson[User](json)
    restored shouldBe User("Diana", "Prince")
  }

  test("unknown fields are ignored") {
    val json     = """{"name":"Alice","age":30,"extra":"ignored","another":123}"""
    val restored = ObjectWeaver.fromJson[Person](json)
    restored shouldBe Person("Alice", 30)
  }

  test("missing required field throws error") {
    val json   = """{"name":"Alice"}"""
    val result =
      try
        ObjectWeaver.fromJson[Person](json)
        None
      catch
        case e: Exception =>
          Some(e)
    result.isDefined shouldBe true
    result.get.getMessage.contains("Missing required field") shouldBe true
  }

  test("null value for required field throws error") {
    val json   = """{"name":"Alice","age":null}"""
    val result =
      try
        ObjectWeaver.fromJson[Person](json)
        None
      catch
        case e: Exception =>
          Some(e)
    result.isDefined shouldBe true
    result.get.getMessage.contains("Null value not allowed for non-optional field") shouldBe true
  }

  test("explicitly given weaver works") {
    case class SimpleData(value: Int)
    given ObjectWeaver[SimpleData] = ObjectWeaver.derived[SimpleData]

    val data     = SimpleData(42)
    val msgpack  = ObjectWeaver.weave(data)
    val restored = ObjectWeaver.unweave[SimpleData](msgpack)
    restored shouldBe data
  }

end CaseClassWeaverTest
