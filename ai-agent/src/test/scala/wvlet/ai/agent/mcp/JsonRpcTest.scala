package wvlet.ai.agent.mcp

import wvlet.airspec.AirSpec
import wvlet.airframe.codec.MessageCodec
import wvlet.ai.agent.core.AIException

class JsonRpcTest extends AirSpec:

  test("parse JSON-RPC request") {
    val json = """{"jsonrpc":"2.0","id":"123","method":"test","params":{"foo":"bar"}}"""

    JsonRpc.parse(json) shouldMatch { case request: JsonRpc.Request =>
      request.jsonrpc shouldBe "2.0"
      request.id shouldBe Some("123")
      request.method shouldBe "test"
      request.params shouldBe Some(Map("foo" -> "bar"))
    }
  }

  test("parse JSON-RPC response with result") {
    val json = """{"jsonrpc":"2.0","id":"123","result":{"status":"ok"}}"""

    JsonRpc.parse(json) shouldMatch { case response: JsonRpc.Response =>
      response.jsonrpc shouldBe "2.0"
      response.id shouldBe Some("123")
      response.result shouldBe Some(Map("status" -> "ok"))
      response.error shouldBe None
    }
  }

  test("parse JSON-RPC response with error") {
    val json =
      """{"jsonrpc":"2.0","id":"123","error":{"code":-32601,"message":"Method not found"}}"""

    JsonRpc.parse(json) shouldMatch { case response: JsonRpc.Response =>
      response.jsonrpc shouldBe "2.0"
      response.id shouldBe Some("123")
      response.result shouldBe None
      response.error shouldMatch { case Some(error) =>
        error.code shouldBe -32601
        error.message shouldBe "Method not found"
      }
    }
  }

  test("parse JSON-RPC notification") {
    val json = """{"jsonrpc":"2.0","method":"notify","params":{"event":"update"}}"""

    JsonRpc.parse(json) shouldMatch { case notification: JsonRpc.Notification =>
      notification.jsonrpc shouldBe "2.0"
      notification.method shouldBe "notify"
      notification.params shouldBe Some(Map("event" -> "update"))
    }
  }

  test("handle parse error") {
    val json = """{"invalid json"""

    val ex = intercept[AIException] {
      JsonRpc.parse(json)
    }
    ex.message shouldContain "Failed to parse JSON"
    ex.statusCode.isUserError shouldBe true
  }

  test("handle invalid JSON-RPC message format") {
    val json = """{"jsonrpc":"2.0"}"""

    val ex = intercept[AIException] {
      JsonRpc.parse(json)
    }
    ex.message shouldContain "Invalid JSON-RPC message format"
    ex.statusCode.isUserError shouldBe true
  }

  test("create request with params") {
    val request = JsonRpc.Request(
      id = Some("test-1"),
      method = "tools/call",
      params = Some(Map("name" -> "calculator", "args" -> Map("x" -> 1, "y" -> 2)))
    )

    val json = MessageCodec.toJson(request)
    json shouldContain "\"jsonrpc\":\"2.0\""
    json shouldContain "\"id\":\"test-1\""
    json shouldContain "\"method\":\"tools/call\""
    json shouldContain "\"params\""
  }

  test("create notification") {
    val notification = JsonRpc.Notification(
      method = "log",
      params = Some(Map("level" -> "info", "message" -> "test"))
    )

    val json = MessageCodec.toJson(notification)
    json shouldContain "\"jsonrpc\":\"2.0\""
    json shouldContain "\"method\":\"log\""
    json shouldNotContain "\"id\""
  }

end JsonRpcTest
