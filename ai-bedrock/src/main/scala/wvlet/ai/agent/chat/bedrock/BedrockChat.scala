package wvlet.ai.agent.chat.bedrock

import software.amazon.awssdk.auth.credentials.{AwsCredentialsProvider, DefaultCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.model.{
  ConverseStreamRequest,
  InferenceConfiguration,
  SystemContentBlock,
  Tool,
  ToolConfiguration,
  ToolInputSchema,
  ToolSpecification
}
import software.amazon.awssdk.services.bedrockruntime.{
  BedrockRuntimeAsyncClient,
  BedrockRuntimeAsyncClientBuilder
}
import wvlet.ai.agent.LLMAgent
import wvlet.ai.agent.chat.{ChatModel, ChatObserver, ChatRequest, ToolSpec}
import wvlet.ai.core.StatusCode
import wvlet.log.LogSupport

import scala.collection.immutable.ListMap
import scala.jdk.CollectionConverters.*

case class BedrockConfig(
    region: Region = Region.US_EAST_1,
    credentialProvider: AwsCredentialsProvider = DefaultCredentialsProvider.create(),
    asyncClientConfig: BedrockRuntimeAsyncClientBuilder => BedrockRuntimeAsyncClientBuilder =
      identity
)

class BedrockChat(agent: LLMAgent, config: BedrockConfig) extends ChatModel with LogSupport:

  import wvlet.ai.core.ChainingUtil.*

  private val client = BedrockRuntimeAsyncClient
    .builder()
    .region(config.region)
    .credentialsProvider(config.credentialProvider)
    .pipe(config.asyncClientConfig(_))
    .build()

  override def chat(request: ChatRequest): Unit                               = ???
  override def chatStream(request: ChatRequest, observer: ChatObserver): Unit = ???

  private[bedrock] def newConverseRequest(request: ChatRequest): ConverseStreamRequest =

    val builder = ConverseStreamRequest.builder().modelId(agent.model.id)

    // Reasoning config
    builder.ifDefined(agent.modelConfig.reasoningConfig) { (builder, config) =>
      builder.additionalModelRequestFields(
        DocumentUtil.fromMap(
          Map("thinking" -> Map("type" -> "enabled", "budget_tokens" -> config.reasoningBudget))
        )
      )
    }

    // Set a system prompt
    builder.ifDefined(agent.systemPrompt) { (builder, systemPrompt) =>
      builder.system(SystemContentBlock.builder().text(systemPrompt).build())
    }

    // Set inference configuration
    val inferenceConfig = InferenceConfiguration
      .builder()
      .ifDefined(agent.modelConfig.maxOutputTokens) { (builder, maxTokens) =>
        if maxTokens > 8192 then
          throw StatusCode
            .INVALID_MODEL_CONFIG
            .newException(s"maxTokens is limited to 8129 in Bedrock, but ${maxTokens} is given")
        builder.maxTokens(maxTokens.toInt)
      }
      .ifDefined(agent.modelConfig.temperature) { (builder, temperature) =>
        builder.temperature(temperature.toFloat)
      }
      .ifDefined(agent.modelConfig.topP) { (builder, topP) =>
        builder.topP(topP.toFloat)
      }
      .ifDefined(agent.modelConfig.topK) { (builder, topK) =>
        warn(s"Ignoring top-k parameter ${topK}, which is not supported in Bedrock")
        builder
      }
      .ifDefined(agent.modelConfig.stopSequences) { (builder, stopSequences) =>
        builder.stopSequences(stopSequences.asJava)
      }
      .build()

    builder.inferenceConfig(inferenceConfig)

    // Set up tools
    val tools = agent
      .tools
      .map { tool =>
        Tool.fromToolSpec(
          ToolSpecification
            .builder()
            .name(tool.name)
            .description(tool.description)
            .inputSchema(extractToolInputSchema(tool))
            .build()
        )
      }
    builder.toolConfig(ToolConfiguration.builder().tools(tools.asJava).build())

    builder.build()

  end newConverseRequest

  private[bedrock] def extractToolInputSchema(tool: ToolSpec): ToolInputSchema =
    // Convert the tool's input schema to Bedrock's ToolInputSchema format
    val properties: ListMap[String, Map[String, Any]] =
      val m = ListMap.newBuilder[String, Map[String, Any]]
      tool
        .parameters
        .foreach { p =>
          m += p.name ->
            Map[String, Any]("type" -> p.dataType.jsonType, "description" -> p.description)
        }
      m.result()

    val requiredParams: Seq[String] = tool
      .parameters
      .filter { p =>
        p.dataType.isRequired
      }
      .map(_.name)

    val inputSchema = ToolInputSchema.fromJson(
      DocumentUtil.fromMap(
        Map(
          "type"       -> "object",
          "properties" -> properties,
          "required"   -> DocumentUtil.fromArray(requiredParams)
        )
      )
    )
    inputSchema

end BedrockChat
