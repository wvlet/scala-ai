package wvlet.ai.agent.chat.genai

import com.google.cloud.ai.generativelanguage.v1beta.{
  Content,
  FunctionCall,
  FunctionResponse,
  GenerateContentRequest,
  GenerateContentResponse,
  GenerationConfig,
  Part,
  SafetySetting,
  Tool => GoogleTool,
  ToolConfig
}
import com.google.cloud.ai.generativelanguage.v1beta.Content.Role
import com.google.api.gax.rpc.{ResponseObserver, StreamController}
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
  ChatEvent,
  ChatFinishReason,
  ChatMessage,
  ChatModel,
  ChatObserver,
  ChatRequest,
  ChatResponse,
  ChatRole,
  ChatStats
}
import wvlet.ai.core.StatusCode
import wvlet.log.LogSupport

import java.util.concurrent.atomic.AtomicReference
import scala.jdk.CollectionConverters.*
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration.*
import scala.util.{Failure, Success, Try}

/**
 * Google GenAI implementation of ChatModel
 */
class GenAIChat(agent: LLMAgent, client: GenAIClient) extends ChatModel with LogSupport:
  import GenAIChat.*
  
  override def chat(request: ChatRequest): Unit = ???
  
  override def chatStream(request: ChatRequest, observer: ChatObserver): ChatResponse =
    val startTime = System.currentTimeMillis()
    val genaiRequest = buildGenerateContentRequest(request)
    
    val responseBuilder = GenAIResponseBuilder(observer)
    val responseObserver = new ResponseObserver[GenerateContentResponse]:
      private var streamController: StreamController = null
      
      override def onStart(controller: StreamController): Unit =
        this.streamController = controller
        
      override def onResponse(response: GenerateContentResponse): Unit =
        responseBuilder.processResponse(response)
        
      override def onError(t: Throwable): Unit =
        observer.onError(t)
        responseBuilder.setError(t)
        
      override def onComplete(): Unit =
        // Finalize the response
        val endTime = System.currentTimeMillis()
        val latency = endTime - startTime
        responseBuilder.setLatency(latency)
        responseBuilder.complete()
    
    // Start streaming
    try
      val stream = client.generateContentStream(genaiRequest, responseObserver)
      stream.send(genaiRequest)
      stream.closeSend()
      
      // Wait for completion
      val response = responseBuilder.getResponse()
      response.getOrElse {
        throw StatusCode.INCOMPLETE_CHAT.newException("Chat response is not available")
      }
    catch
      case e: Exception =>
        observer.onError(e)
        throw e
  
  /**
   * Build a GenerateContentRequest from a ChatRequest
   */
  private def buildGenerateContentRequest(request: ChatRequest): GenerateContentRequest =
    val requestBuilder = GenerateContentRequest.newBuilder()
    
    // Set the model
    requestBuilder.setModel(s"models/${agent.model.id}")
    
    // Add system prompt if available
    if agent.systemPrompt.isDefined then
      val systemContent = Content.newBuilder()
        .setRole(Role.SYSTEM.toString)
        .addParts(Part.newBuilder().setText(agent.systemPrompt.get).build())
        .build()
      requestBuilder.addContents(systemContent)
    
    // Add messages
    request.messages.foreach { message =>
      val content = convertChatMessageToContent(message)
      if content != null then
        requestBuilder.addContents(content)
    }
    
    // Configure generation parameters
    val generationConfigBuilder = GenerationConfig.newBuilder()
    
    // Apply model configuration
    val modelConfig = request.overrideConfig.getOrElse(agent.modelConfig)
    
    modelConfig.temperature.foreach { temp =>
      generationConfigBuilder.setTemperature(temp.toFloat)
    }
    
    modelConfig.topP.foreach { topP =>
      generationConfigBuilder.setTopP(topP.toFloat)
    }
    
    modelConfig.topK.foreach { topK =>
      generationConfigBuilder.setTopK(topK)
    }
    
    modelConfig.maxOutputTokens.foreach { maxTokens =>
      generationConfigBuilder.setMaxOutputTokens(maxTokens)
    }
    
    modelConfig.stopSequences.foreach { sequences =>
      generationConfigBuilder.addAllStopSequences(sequences.asJava)
    }
    
    requestBuilder.setGenerationConfig(generationConfigBuilder.build())
    
    // Add tools if available
    if agent.tools.nonEmpty then
      val toolConfigBuilder = ToolConfig.newBuilder()
      
      // Convert tools to function declarations
      agent.tools.foreach { tool =>
        val functionDeclaration = client.convertToolToFunctionDeclaration(tool)
        val googleTool = GoogleTool.newBuilder()
          .setFunctionDeclarations(
            GoogleTool.FunctionDeclarations.newBuilder()
              .addFunctionDeclarations(functionDeclaration)
              .build()
          )
          .build()
        toolConfigBuilder.addTools(googleTool)
      }
      
      requestBuilder.setToolConfig(toolConfigBuilder.build())
    
    requestBuilder.build()

object GenAIChat extends LogSupport:
  /**
   * Convert a ChatMessage to a Google GenAI Content
   */
  private def convertChatMessageToContent(message: ChatMessage): Content =
    val contentBuilder = Content.newBuilder()
    
    message match
      case SystemMessage(text) =>
        contentBuilder.setRole(Role.SYSTEM.toString)
        contentBuilder.addParts(Part.newBuilder().setText(text).build())
        
      case UserMessage(text) =>
        contentBuilder.setRole(Role.USER.toString)
        contentBuilder.addParts(Part.newBuilder().setText(text).build())
        
      case AIMessage(text, toolCalls) =>
        contentBuilder.setRole(Role.MODEL.toString)
        contentBuilder.addParts(Part.newBuilder().setText(text).build())
        
        // Add tool calls if any
        toolCalls.foreach { toolCall =>
          val functionCall = FunctionCall.newBuilder()
            .setName(toolCall.name)
            .putAllArgs(toolCall.args.map { case (k, v) => k -> v.toString }.asJava)
            .build()
          
          val part = Part.newBuilder()
            .setFunctionCall(functionCall)
            .build()
          
          contentBuilder.addParts(part)
        }
        
      case AIReasoningMessage(text) =>
        contentBuilder.setRole(Role.MODEL.toString)
        contentBuilder.addParts(Part.newBuilder().setText(text).build())
        
      case ToolResultMessage(id, toolName, text) =>
        contentBuilder.setRole(Role.FUNCTION.toString)
        
        val functionResponse = FunctionResponse.newBuilder()
          .setName(toolName)
          .setResponse(text)
          .build()
        
        val part = Part.newBuilder()
          .setFunctionResponse(functionResponse)
          .build()
        
        contentBuilder.addParts(part)
        
      case _ =>
        warn(s"Unsupported message type: ${message.getClass.getName}")
        return null
    
    contentBuilder.build()
  
  /**
   * Determine the finish reason from the response
   */
  private def determineFinishReason(response: GenerateContentResponse): ChatFinishReason =
    if response.getCandidatesCount == 0 then
      return ChatFinishReason.UNKNOWN
    
    val candidate = response.getCandidates(0)
    
    if candidate.hasFinishReason then
      candidate.getFinishReason match
        case "STOP" => ChatFinishReason.END_TURN
        case "MAX_TOKENS" => ChatFinishReason.MAX_TOKENS
        case "SAFETY" => ChatFinishReason.CONTENT_FILTERED
        case "RECITATION" => ChatFinishReason.CONTENT_FILTERED
        case "TOOL_CALLS" => ChatFinishReason.TOOL_CALL
        case _ => ChatFinishReason.UNKNOWN
    else
      ChatFinishReason.END_TURN