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
import wvlet.ai.agent.chat.ChatEvent.{
  PartialReasoningResponse,
  PartialResponse,
  PartialToolRequestResponse
}
import wvlet.ai.agent.chat.ChatObserver

import scala.jdk.CollectionConverters.*
import wvlet.ai.core.ops.*

/**
  * Accumulate the response from a Bedrock chat stream, and build ConverseResponse from it.
  */
class BedrockConverseResponseBuilder(observer: ChatObserver):
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
        val text = delta.toolUse().input()
        toolUseInput.append(text)
        observer.onPartialResponse(PartialToolRequestResponse(text))
      case ContentBlockDelta.Type.TEXT =>
        val text = delta.text()
        chatText.append(text)
        observer.onPartialResponse(PartialResponse(text))
      case ContentBlockDelta.Type.REASONING_CONTENT =>
        val text = delta.reasoningContent().text()
        reasoningText.append(text)
        observer.onPartialResponse(PartialReasoningResponse(text))

  def onEvent(event: ContentBlockStopEvent): Unit = toolUseBlockBuilder.foreach { builder =>
    if toolUseInput.nonEmpty then
      builder.input(DocumentUtil.fromJson(toolUseInput.result()))
      toolUseInput.clear()
      toolUseBlockBuilder = None
  }

  def onEvent(event: ConverseStreamMetadataEvent): Unit =
    converseResponseBuilder.usage(event.usage())
    converseResponseBuilder.metrics(builder =>
      builder.latencyMs(event.metrics().latencyMs()).build()
    )
    // Finalize the response as the metadata is reported at the end of the chat
    val chatResponse = BedrockChat.buildChatResponseFrom(converseResponseBuilder.build())
    observer.onComplete(chatResponse)

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
    // Reset the builder
    messageBuilder = Message.builder()

end BedrockConverseResponseBuilder
