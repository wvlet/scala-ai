package wvlet.ai.agent.tool

import wvlet.ai.agent.chat.ChatMessage
import wvlet.ai.agent.chat.ToolSpec
import wvlet.ai.agent.chat.ChatMessage.ToolCallRequest
import wvlet.ai.agent.chat.ChatMessage.ToolResultMessage
import wvlet.airframe.rx.Rx

/**
  * Trait for executing tool calls. Implementations of this trait handle the actual invocation of
  * tools and return the results.
  */
trait ToolExecutor:
  /**
    * Execute a tool call and return the result asynchronously.
    *
    * @param toolCall
    *   The tool call request containing the tool name and arguments
    * @return
    *   An Rx stream that emits the tool result message
    */
  def executeToolCall(toolCall: ToolCallRequest): Rx[ToolResultMessage]

  /**
    * Get the list of available tools that this executor can handle.
    *
    * @return
    *   The list of tool specifications
    */
  def availableTools: Seq[ToolSpec]

  /**
    * Execute multiple tool calls in parallel.
    *
    * @param toolCalls
    *   The list of tool calls to execute
    * @return
    *   An Rx stream that emits all tool results
    */
  def executeToolCalls(toolCalls: Seq[ToolCallRequest]): Rx[Seq[ToolResultMessage]] =
    // Execute all tool calls in parallel and collect results
    val rxResults = toolCalls.map(executeToolCall)
    Rx.zip(rxResults)

  /**
    * Find a tool specification by name.
    *
    * @param name
    *   The tool name
    * @return
    *   The tool specification if found
    */
  def findTool(name: String): Option[ToolSpec] = availableTools.find(_.name == name)

end ToolExecutor
