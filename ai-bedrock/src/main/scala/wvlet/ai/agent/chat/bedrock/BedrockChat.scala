package wvlet.ai.agent.chat.bedrock

import software.amazon.awssdk.auth.credentials.{AwsCredentialsProvider, DefaultCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.model.{
  ContentBlock,
  ConversationRole,
  ConverseStreamRequest,
  ConverseStreamResponseHandler,
  InferenceConfiguration,
  Message,
  SystemContentBlock,
  Tool,
  ToolConfiguration,
  ToolInputSchema,
  ToolResultBlock,
  ToolResultContentBlock,
  ToolSpecification
}
import software.amazon.awssdk.services.bedrockruntime.{
  BedrockRuntimeAsyncClient,
  BedrockRuntimeAsyncClientBuilder
}
import wvlet.ai.agent.LLMAgent
import wvlet.ai.agent.chat.ChatMessage.{AIMessage, SystemMessage, ToolMessage, UserMessage}
import wvlet.ai.agent.chat.{ChatMessage, ChatModel, ChatObserver, ChatRequest, ToolSpec}
import wvlet.ai.core.StatusCode
import wvlet.log.LogSupport

import java.util.concurrent.atomic.AtomicReference
import scala.collection.immutable.ListMap
import scala.jdk.CollectionConverters.*

case class BedrockConfig(
    region: Region = Region.US_EAST_1,
    credentialProvider: AwsCredentialsProvider = DefaultCredentialsProvider.create(),
    asyncClientConfig: BedrockRuntimeAsyncClientBuilder => BedrockRuntimeAsyncClientBuilder =
      identity
)

class BedrockChat(agent: LLMAgent, config: BedrockConfig) extends ChatModel with LogSupport:
  import BedrockChat.*
  import wvlet.ai.core.ops.*

  private val client = BedrockRuntimeAsyncClient
    .builder()
    .region(config.region)
    .credentialsProvider(config.credentialProvider)
    .pipe(config.asyncClientConfig(_))
    .build()

  override def chat(request: ChatRequest): Unit = ???
  override def chatStream(request: ChatRequest, observer: ChatObserver): Unit =
    val converseRequest = newConverseRequest(request)
    // val finalResponse = AtomicReference[]()

    val chatStreamHandler = BedrockChatStreamHandler(observer)
    val chatStreamResponseHandler = ConverseStreamResponseHandler
      .builder()
      .subscriber(
        ConverseStreamResponseHandler
          .Visitor
          .builder()
          .onContentBlockStart(chatStreamHandler.onEvent)
          .onContentBlockDelta(chatStreamHandler.onEvent)
          .onContentBlockStop(chatStreamHandler.onEvent)
          .onMetadata(chatStreamHandler.onEvent)
          .onMessageStart(chatStreamHandler.onEvent)
          .build()
      )
      .build()

    val future = client.converseStream(converseRequest, chatStreamResponseHandler)
    future.get()

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

    // Set messages
    val messages: Seq[Message] = extractBedrockChatMessages(request.messages)
    builder.messages(messages.asJava)

    builder.build()

  end newConverseRequest

end BedrockChat

object BedrockChat:
  private[bedrock] def extractBedrockChatMessages(messages: Seq[ChatMessage]): Seq[Message] =
    val bedrockMessages = Seq.newBuilder[Message]
    val contentBlocks   = Seq.newBuilder[ContentBlock]
    messages
      .zipWithIndex
      .foreach { case (message, index) =>
        message match
          case s: SystemMessage =>
          // skip
          case u: UserMessage =>
            bedrockMessages +=
              Message
                .builder()
                .role(ConversationRole.USER)
                .content(ContentBlock.fromText(u.text))
                .build()
          case m: AIMessage =>
            bedrockMessages +=
              Message
                .builder()
                .role(ConversationRole.ASSISTANT)
                .content(ContentBlock.fromText(m.text))
                .build()
          case t: ToolMessage =>
            val contentBlock = ContentBlock
              .builder()
              .toolResult(
                ToolResultBlock
                  .builder()
                  .toolUseId(t.id)
                  .content(ToolResultContentBlock.builder().text(t.text).build())
                  .build()
              )
              .build()
            contentBlocks += contentBlock

            val isLastOrNextIsNotToolMessage =
              index + 1 >= messages.size || {
                messages(index + 1) match
                  case _: ToolMessage =>
                    false
                  case _ =>
                    true
              }

            if isLastOrNextIsNotToolMessage then
              bedrockMessages +=
                Message
                  .builder()
                  .role(ConversationRole.USER)
                  .content(contentBlocks.result().asJava)
                  .build()
              contentBlocks.clear()
          case other =>
            throw StatusCode
              .INVALID_MESSAGE_TYPE
              .newException(s"Unsupported message type: ${other}")
      }

    bedrockMessages.result()

  end extractBedrockChatMessages

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
