package wvlet.ai.agent.chat.bedrock

import software.amazon.awssdk.auth.credentials.{AwsCredentialsProvider, DefaultCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.model.{
  ContentBlock,
  ConversationRole,
  ConverseOutput,
  ConverseResponse,
  ConverseStreamRequest,
  ConverseStreamResponseHandler,
  InferenceConfiguration,
  Message,
  ReasoningContentBlock,
  ReasoningTextBlock,
  StopReason,
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
import wvlet.ai.agent.chat.ChatMessage.{
  AIMessage,
  AIReasoningMessage,
  SystemMessage,
  ToolCallRequest,
  ToolResultMessage,
  UserMessage
}
import wvlet.ai.agent.chat.{
  ChatFinishReason,
  ChatMessage,
  ChatModel,
  ChatObserver,
  ChatRequest,
  ChatResponse,
  ChatRole,
  ChatStats,
  ToolSpec
}
import wvlet.ai.agent.core.StatusCode
import wvlet.log.LogSupport

import java.util.concurrent.atomic.AtomicReference
import scala.collection.immutable.ListMap
import scala.jdk.CollectionConverters.*

class BedrockChat(agent: LLMAgent, bedrockClient: BedrockClient) extends ChatModel with LogSupport:
  import BedrockChat.*
  import wvlet.ai.agent.core.ops.*

  override def chat(request: ChatRequest): Unit = ???
  override def chatStream(request: ChatRequest, observer: ChatObserver): ChatResponse =
    val converseRequest = newConverseRequest(request)

    val converseResponseBuilder = BedrockConverseResponseBuilder(observer)
    val chatStreamResponseHandler = ConverseStreamResponseHandler
      .builder()
      .subscriber(
        ConverseStreamResponseHandler
          .Visitor
          .builder()
          .onContentBlockStart(converseResponseBuilder.onEvent)
          .onContentBlockDelta(converseResponseBuilder.onEvent)
          .onContentBlockStop(converseResponseBuilder.onEvent)
          .onMetadata(converseResponseBuilder.onEvent)
          .onMessageStart(converseResponseBuilder.onEvent)
          .onMessageStop(converseResponseBuilder.onEvent)
          .build()
      )
      .onError(e => observer.onError(e))
      .build()

    val future = bedrockClient.converseStream(converseRequest, chatStreamResponseHandler)
    future.get()
    converseResponseBuilder
      .getResponse
      .getOrElse {
        throw StatusCode.INCOMPLETE_CHAT.newException("Chat response is not available")
      }

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
    if tools.nonEmpty then
      builder.toolConfig(ToolConfiguration.builder().tools(tools.asJava).build())

    // Set messages
    val messages: Seq[Message] = extractBedrockChatMessages(request.messages)
    builder.messages(messages.asJava)

    builder.build()

  end newConverseRequest

end BedrockChat

object BedrockChat extends LogSupport:
  private[bedrock] def buildChatResponseFrom(converseResponse: ConverseResponse): ChatResponse =
    val finishReason: ChatFinishReason = fromBedrockStopReason(converseResponse.stopReason())
    val chatStats: ChatStats = ChatStats(
      latencyMs = converseResponse.metrics().latencyMs(),
      inputTokens = converseResponse.usage().inputTokens(),
      outputTokens = converseResponse.usage().outputTokens(),
      totalTokens = converseResponse.usage().totalTokens()
    )

    val messages = fromBedrockOutput(converseResponse.output())

    ChatResponse(messages = messages, stats = chatStats, finishReason = finishReason)

  private[bedrock] def fromBedrockStopReason(stopReason: StopReason): ChatFinishReason =
    stopReason match
      case StopReason.TOOL_USE =>
        ChatFinishReason.TOOL_CALL
      case StopReason.END_TURN =>
        ChatFinishReason.END_TURN
      case StopReason.STOP_SEQUENCE =>
        ChatFinishReason.STOP_SEQUENCE
      case StopReason.CONTENT_FILTERED =>
        ChatFinishReason.CONTENT_FILTERED
      case StopReason.MAX_TOKENS =>
        ChatFinishReason.MAX_TOKENS
      case StopReason.UNKNOWN_TO_SDK_VERSION =>
        ChatFinishReason.UNKNOWN
      case other =>
        warn(s"Unknown stop reason: ${other}")
        ChatFinishReason.UNKNOWN

  private[bedrock] def fromBedrockOutput(output: ConverseOutput): Seq[ChatMessage] =
    val messages         = Seq.newBuilder[ChatMessage]
    val role             = output.message().role()
    val toolCallRequests = Seq.newBuilder[ToolCallRequest]
    val textContent      = StringBuilder()
    val contents         = output.message().content().asScala
    contents.foreach { block =>
      block.`type`() match
        case ContentBlock.Type.TEXT =>
          textContent.append(block.text())
        case ContentBlock.Type.REASONING_CONTENT =>
          messages += AIReasoningMessage(block.reasoningContent().reasoningText().text())
        case ContentBlock.Type.TOOL_USE =>
          toolCallRequests +=
            ToolCallRequest(
              id = block.toolUse().toolUseId(),
              name = block.toolUse().name(),
              args = DocumentUtil.toMap(block.toolUse().input())
            )
        case _ =>
          throw StatusCode
            .INVALID_MESSAGE_TYPE
            .newException(s"Unsupported message type: ${block.`type`()}")
    }

    import wvlet.ai.agent.core.ops.*
    toolCallRequests
      .result()
      .pipe { toolCallRequests =>
        if toolCallRequests.nonEmpty then
          messages += AIMessage(textContent.result(), toolCallRequests)
        else
          role match
            case ConversationRole.USER =>
              messages += UserMessage(textContent.result())
            case ConversationRole.ASSISTANT =>
              messages += AIMessage(textContent.result())
            case _ =>
              warn(s"Unknown role: ${role}. Use USER role")
              messages += UserMessage(textContent.result())
      }
    messages.result()

  end fromBedrockOutput

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
          case m: AIReasoningMessage =>
            bedrockMessages +=
              Message
                .builder()
                .role(ConversationRole.ASSISTANT)
                .content(
                  ContentBlock.fromReasoningContent(
                    ReasoningContentBlock.fromReasoningText(
                      ReasoningTextBlock.builder().text(m.text).build()
                    )
                  )
                )
                .build()
          case t: ToolResultMessage =>
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
                  case _: ToolResultMessage =>
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
