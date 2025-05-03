package wvlet.ai.agent.chat.bedrock

import wvlet.ai.agent.{LLM, LLMAgent, ModelConfig}
import wvlet.ai.agent.chat.{ChatMessage, ChatRequest, ToolParameter, ToolSpec}
import wvlet.ai.core.DataType
import wvlet.airspec.AirSpec

import scala.jdk.CollectionConverters.*

class BedrockChatToolTest extends AirSpec:

  private def createAgent(modelConfig: ModelConfig = ModelConfig()): LLMAgent = LLMAgent(
    name = "test-agent",
    description = "Test Agent",
    model = LLM.Bedrock.Claude3_7Sonnet_20250219V1_0,
    modelConfig = modelConfig
  )

  private def createChat(agent: LLMAgent): BedrockChat =
    new BedrockChat(agent, BedrockConfig()) // Use default config for testing request building

  private val dummyRequest = ChatRequest(List(ChatMessage.user("Hello")))

  test("should build ConverseStreamRequest with tool configuration") {
    val tool1 = ToolSpec(
      name = "get_weather",
      description = "Get the current weather for a location",
      parameters = List(
        ToolParameter(
          "location",
          "The city and state, e.g., San Francisco, CA",
          DataType.StringType
        ),
        ToolParameter(
          "unit",
          "The temperature unit (celsius or fahrenheit)",
          DataType.OptionalType(DataType.StringType),
          defaultValue = Some("celsius")
        )
      ),
      returnType = DataType.JsonType
    )

    val tool2 = ToolSpec(
      name = "search_web",
      description = "Search the web for a query",
      parameters = List(ToolParameter("query", "The search query", DataType.StringType)),
      returnType = DataType.StringType
    )

    val agent = createAgent().withTools(List(tool1, tool2))
    val chat  = createChat(agent)

    val converseRequest = chat.newConverseRequest(dummyRequest)

    val toolConfig = converseRequest.toolConfig()
    toolConfig shouldNotBe null
    val tools = toolConfig.tools().asScala
    tools.size shouldBe 2

    // Verify Tool 1
    val bedrockTool1 = tools(0).toolSpec()
    bedrockTool1.name() shouldBe "get_weather"
    bedrockTool1.description() shouldBe "Get the current weather for a location"
    val schema1 = DocumentUtil.toMap(bedrockTool1.inputSchema().json())
    // Note: We are assuming DocumentUtil correctly serializes the schema.
    // A more robust test might involve parsing the JSON string if a library is available.
    schema1("type") shouldBe "object"
    schema1("properties") shouldMatch { case props1: Map[String, Any] =>
      props1("location") shouldMatch { case location: Map[String, Any] =>
        location("type") shouldBe "string"
        location("description") shouldBe "The city and state, e.g., San Francisco, CA"
      }
      props1("unit") shouldMatch { case unit: Map[String, Any] =>
        unit("type") shouldBe "string" // OptionalType maps to base type
        unit("description") shouldBe "The temperature unit (celsius or fahrenheit)"
      }
    }
    schema1("required") shouldMatch { case required1: Seq[String] =>
      required1 shouldContain "location"
      required1 shouldNotContain "unit" // Optional parameter
    }

    // Verify Tool 2
    val bedrockTool2 = tools(1).toolSpec()
    bedrockTool2.name() shouldBe "search_web"
    bedrockTool2.description() shouldBe "Search the web for a query"
    val schema2 = DocumentUtil.toMap(bedrockTool2.inputSchema().json())
    schema2("type") shouldBe "object"
    schema2("properties") shouldMatch { case props2: Map[String, Any] =>
      props2("query") shouldMatch { case query: Map[String, Any] =>
        query("type") shouldBe "string"
        query("description") shouldBe "The search query"
      }
    }
    schema2("required") shouldMatch { case required2: Seq[String] =>
      required2.size shouldBe 1
      required2 shouldContain "query"
    }
  }

end BedrockChatToolTest
