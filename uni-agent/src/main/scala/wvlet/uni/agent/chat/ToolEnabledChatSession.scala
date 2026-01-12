package wvlet.uni.agent.chat

import wvlet.uni.agent.chat.ChatMessage.AIMessage
import wvlet.uni.agent.chat.ChatMessage.ToolCallRequest
import wvlet.uni.agent.chat.ChatMessage.ToolResultMessage
import wvlet.uni.agent.tool.ToolExecutor
import wvlet.airframe.rx.Rx
import wvlet.uni.log.LogSupport

/**
  * A chat session that can automatically execute tool calls using a ToolExecutor. This trait
  * extends the basic ChatSession to add tool execution capabilities.
  */
trait ToolEnabledChatSession extends ChatSession with LogSupport:
  /**
    * The tool executor used to handle tool calls.
    */
  def toolExecutor: Option[ToolExecutor]

  /**
    * Enable automatic tool execution for this session.
    *
    * @param executor
    *   The tool executor to use
    * @return
    *   A new session with tool execution enabled
    */
  def withToolExecutor(executor: ToolExecutor): ToolEnabledChatSession

  /**
    * Disable automatic tool execution for this session.
    *
    * @return
    *   A new session with tool execution disabled
    */
  def withoutToolExecutor: ToolEnabledChatSession

  /**
    * Chat with automatic tool execution. If the model returns tool calls, they will be executed
    * automatically and the results will be sent back to the model to get the final response.
    *
    * @param message
    *   The user message
    * @param maxToolRounds
    *   Maximum number of tool execution rounds (default: 10)
    * @return
    *   The final chat response after all tool executions
    */
  def chatWithTools(message: String, maxToolRounds: Int = 10): Rx[ChatResponse] = chatWithTools(
    ChatRequest(messages = Seq(ChatMessage.user(message))),
    maxToolRounds
  )

  /**
    * Chat with automatic tool execution using a ChatRequest.
    *
    * @param request
    *   The chat request
    * @param maxToolRounds
    *   Maximum number of tool execution rounds
    * @return
    *   The final chat response after all tool executions
    */
  def chatWithTools(request: ChatRequest, maxToolRounds: Int): Rx[ChatResponse] =
    def executeRound(req: ChatRequest, round: Int): Rx[ChatResponse] =
      if round >= maxToolRounds then
        Rx.single(
          ChatResponse.errorResponse(
            s"Reached maximum tool execution rounds ($maxToolRounds)",
            ChatFinishReason.MAX_ROUNDS
          )
        )
      else
        Rx.single(chatStream(req))
          .flatMap { response =>
            // Check if the response contains tool calls
            response
              .messages
              .collectFirst { case ai: AIMessage =>
                ai
              } match
              case Some(aiMessage) if aiMessage.toolCalls.nonEmpty =>
                debug(s"Executing ${aiMessage.toolCalls.size} tool calls in round $round")
                executeToolCalls(aiMessage.toolCalls).flatMap { toolResults =>
                  // Create a new request with the tool results
                  val newMessages = req.messages ++ Seq(aiMessage) ++ toolResults
                  val newRequest  = req.copy(messages = newMessages)
                  // Continue the conversation with tool results
                  executeRound(newRequest, round + 1)
                }
              case _ =>
                // No tool calls, return the response as is
                Rx.single(response)
          }

    executeRound(request, 0)

  end chatWithTools

  /**
    * Execute tool calls and return the results.
    *
    * @param toolCalls
    *   The tool calls to execute
    * @return
    *   An Rx stream that emits the tool results
    */
  def executeToolCalls(toolCalls: Seq[ToolCallRequest]): Rx[Seq[ToolResultMessage]] =
    toolExecutor match
      case Some(executor) =>
        debug(s"Executing ${toolCalls.size} tool calls: ${toolCalls.map(_.name).mkString(", ")}")
        executor.executeToolCalls(toolCalls)
      case None =>
        warn("No tool executor configured, returning error results")
        Rx.single(
          toolCalls.map { toolCall =>
            ToolResultMessage(
              id = toolCall.id,
              toolName = toolCall.name,
              text = """{"error": "No tool executor configured"}"""
            )
          }
        )

end ToolEnabledChatSession

/**
  * Default implementation of ToolEnabledChatSession that wraps an existing ChatSession.
  */
class ToolEnabledChatSessionImpl(
    underlying: ChatSession,
    override val toolExecutor: Option[ToolExecutor] = None
) extends ToolEnabledChatSession:

  override def chatStream(request: ChatRequest, observer: ChatObserver): ChatResponse = underlying
    .chatStream(request, observer)

  override def withToolExecutor(executor: ToolExecutor): ToolEnabledChatSession =
    new ToolEnabledChatSessionImpl(underlying, Some(executor))

  override def withoutToolExecutor: ToolEnabledChatSession =
    new ToolEnabledChatSessionImpl(underlying, None)

end ToolEnabledChatSessionImpl

/**
  * Extension methods for ChatSession to add tool execution capabilities.
  */
extension (session: ChatSession)
  /**
    * Convert a regular ChatSession to a ToolEnabledChatSession.
    *
    * @param executor
    *   Optional tool executor
    * @return
    *   A ToolEnabledChatSession
    */
  def withToolSupport(executor: Option[ToolExecutor] = None): ToolEnabledChatSession =
    session match
      case toolEnabled: ToolEnabledChatSession =>
        executor match
          case Some(exec) =>
            toolEnabled.withToolExecutor(exec)
          case None =>
            toolEnabled
      case _ =>
        new ToolEnabledChatSessionImpl(session, executor)
