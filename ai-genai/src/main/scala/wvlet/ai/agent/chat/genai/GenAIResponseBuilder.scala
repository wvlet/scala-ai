package wvlet.ai.agent.chat.genai

import com.google.cloud.ai.generativelanguage.v1beta.{
  Content,
  FunctionCall,
  GenerateContentResponse,
  Part
}
import wvlet.ai.agent.chat.ChatMessage.{AIMessage, ToolCallRequest}
import wvlet.ai.agent.chat.{
  ChatEvent,
  ChatFinishReason,
  ChatMessage,
  ChatObserver,
  ChatResponse,
  ChatStats
}
import wvlet.log.LogSupport

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import scala.jdk.CollectionConverters.*
import scala.collection.mutable.ListBuffer

/**
 * Builder for creating ChatResponse from Google GenAI responses
 */
class GenAIResponseBuilder(observer: ChatObserver) extends LogSupport:
  private val responseRef = new AtomicReference[ChatResponse]()
  private val errorRef = new AtomicReference[Throwable]()
  private val isCompleted = new AtomicBoolean(false)
  
  private val messages = ListBuffer[ChatMessage]()
  private val currentText = new StringBuilder()
  private val toolCalls = ListBuffer[ToolCallRequest]()
  
  private var inputTokens: Int = 0
  private var outputTokens: Int = 0
  private var latencyMs: Long = 0
  
  /**
   * Process a response from the Google GenAI API
   */
  def processResponse(response: GenerateContentResponse): Unit =
    if response.getCandidatesCount == 0 then
      return
      
    val candidate = response.getCandidates(0)
    
    // Update token counts
    if candidate.hasUsageMetadata then
      val metadata = candidate.getUsageMetadata
      inputTokens = metadata.getPromptTokenCount
      outputTokens = metadata.getCandidatesTokenCount
    
    // Process content
    if candidate.getContentCount > 0 then
      val content = candidate.getContent(0)
      
      // Process each part
      content.getPartsList.asScala.foreach { part =>
        if part.hasText then
          val text = part.getText
          currentText.append(text)
          observer.onPartialResponse(ChatEvent.PartialResponse(text))
        
        if part.hasFunctionCall then
          val functionCall = part.getFunctionCall
          val toolCall = ToolCallRequest(
            id = java.util.UUID.randomUUID().toString,
            name = functionCall.getName,
            args = functionCall.getArgsMap.asScala.toMap.map { case (k, v) => k -> v }
          )
          toolCalls += toolCall
          observer.onPartialResponse(ChatEvent.PartialToolRequestResponse(functionCall.getName))
      }
  
  /**
   * Complete the response building process
   */
  def complete(): Unit =
    if isCompleted.compareAndSet(false, true) then
      // Create the final message
      if currentText.nonEmpty || toolCalls.nonEmpty then
        val message = AIMessage(currentText.toString, toolCalls.toSeq)
        messages += message
      
      // Calculate total tokens
      val totalTokens = inputTokens + outputTokens
      
      // Create stats
      val stats = ChatStats(
        latencyMs = latencyMs,
        inputTokens = inputTokens,
        outputTokens = outputTokens,
        totalTokens = totalTokens
      )
      
      // Determine finish reason
      val finishReason = if toolCalls.nonEmpty then 
        ChatFinishReason.TOOL_CALL 
      else 
        ChatFinishReason.END_TURN
      
      // Create the response
      val response = ChatResponse(
        messages = messages.toSeq,
        stats = stats,
        finishReason = finishReason
      )
      
      responseRef.set(response)
      observer.onComplete(response)
  
  /**
   * Set an error that occurred during processing
   */
  def setError(error: Throwable): Unit =
    errorRef.set(error)
  
  /**
   * Set the latency of the request
   */
  def setLatency(latencyMs: Long): Unit =
    this.latencyMs = latencyMs
  
  /**
   * Get the built response
   */
  def getResponse(): Option[ChatResponse] =
    Option(responseRef.get())

object GenAIResponseBuilder:
  def apply(observer: ChatObserver): GenAIResponseBuilder = new GenAIResponseBuilder(observer)