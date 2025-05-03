package wvlet.ai.agent.chat.bedrock

import software.amazon.awssdk.services.bedrockruntime.model.{
  ContentBlock,
  ContentBlockDelta,
  ContentBlockDeltaEvent,
  ContentBlockStart,
  ContentBlockStartEvent,
  ContentBlockStopEvent,
  ConverseOutput,
  ConverseResponse,
  ConverseStreamMetadataEvent,
  Message,
  MessageStartEvent,
  MessageStopEvent,
  ToolUseBlock
}
import software.amazon.eventstream.MessageBuilder
import wvlet.ai.agent.chat.ChatObserver

import scala.jdk.CollectionConverters.*
import wvlet.ai.core.ops.*

/**
  * Accumulate the response from a Bedrock chat stream, and build ConverseResponse from it.
  */
class BedrockChatStreamHandler(observer: ChatObserver):
  private val converseResponseBuilder                           = ConverseResponse.builder()
  private var toolUseBlockBuilder: Option[ToolUseBlock.Builder] = None
  private val toolUseBlocks                                     = List.newBuilder[ToolUseBlock]
  private val toolUseInput                                      = StringBuilder()

  private var messageBuilder: Message.Builder = Message.builder()
  private val reasoningText                   = StringBuilder()
  private val chatText                        = StringBuilder()

  def onEvent(event: ContentBlockStartEvent): Unit =
    val startEvent = event.start()
    if startEvent.`type`() == ContentBlockStart.Type.TOOL_USE then
      val toolUse = startEvent.toolUse()
      // Remember the tool call id and tool name
      toolUseBlockBuilder = Some(
        ToolUseBlock.builder().toolUseId(toolUse.toolUseId()).name(toolUse.name())
      )

  def onEvent(event: ContentBlockDeltaEvent): Unit =
    val delta = event.delta()
    delta.`type`() when:
      case ContentBlockDelta.Type.TOOL_USE =>
        toolUseInput.append(delta.toolUse().input())
      case ContentBlockDelta.Type.TEXT =>
        chatText.append(delta.text())
      case ContentBlockDelta.Type.REASONING_CONTENT =>
        reasoningText.append(delta.reasoningContent().text())

  def onEvent(event: ContentBlockStopEvent): Unit = {}

  def onEvent(event: ConverseStreamMetadataEvent): Unit = {}

  def onEvent(event: MessageStartEvent): Unit =
    messageBuilder = Message.builder().role(event.role())

  def onEvent(event: MessageStopEvent): Unit =
    converseResponseBuilder.stopReason(event.stopReason())
    converseResponseBuilder.additionalModelResponseFields(event.additionalModelResponseFields())
    val contents = List.newBuilder[ContentBlock]
    contents += ContentBlock.builder().text(chatText.result()).build()
    contents ++=
      toolUseBlocks
        .result()
        .map { toolUse =>
          ContentBlock.fromToolUse(toolUse)
        }
    converseResponseBuilder.output(
      ConverseOutput
        .builder()
        .message(messageBuilder.content(contents.result().asJava).build())
        .build()
    )
    messageBuilder = Message.builder()

end BedrockChatStreamHandler
