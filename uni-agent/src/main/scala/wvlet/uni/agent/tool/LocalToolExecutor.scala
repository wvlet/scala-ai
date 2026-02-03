package wvlet.uni.agent.tool

import wvlet.uni.agent.chat.ChatMessage
import wvlet.uni.agent.chat.ToolSpec
import wvlet.uni.agent.chat.ChatMessage.ToolCallRequest
import wvlet.uni.agent.chat.ChatMessage.ToolResultMessage
import wvlet.airframe.rx.Rx
import wvlet.airframe.codec.MessageCodec
import wvlet.uni.log.LogSupport

/**
  * A local tool executor that executes tools in-memory using registered functions. This is useful
  * for testing and for simple tool implementations.
  *
  * @param toolRegistry
  *   Map of tool names to their function implementations
  * @param toolSpecs
  *   List of available tool specifications
  */
class LocalToolExecutor(
    private val toolRegistry: Map[String, ToolFunction] = Map.empty,
    private val toolSpecs: Seq[ToolSpec] = Seq.empty
) extends ToolExecutor
    with LogSupport:

  /**
    * Register a tool with its implementation.
    *
    * @param spec
    *   The tool specification
    * @param function
    *   The function that implements the tool
    * @return
    *   This executor for chaining
    */
  def registerTool(spec: ToolSpec, function: ToolFunction): LocalToolExecutor =
    new LocalToolExecutor(toolRegistry.updated(spec.name, function), toolSpecs :+ spec)

  /**
    * Register multiple tools at once.
    *
    * @param tools
    *   Sequence of (ToolSpec, ToolFunction) pairs
    * @return
    *   This executor for chaining
    */
  def registerTools(tools: Seq[(ToolSpec, ToolFunction)]): LocalToolExecutor =
    tools.foldLeft(this) { (executor, tool) =>
      val (spec, function) = tool
      executor.registerTool(spec, function)
    }

  override def executeToolCall(toolCall: ToolCallRequest): Rx[ToolResultMessage] =
    toolRegistry.get(toolCall.name) match
      case Some(function) =>
        debug(s"Executing tool: ${toolCall.name} with args: ${toolCall.args}")
        Rx.single {
          try
            val result = function(toolCall.args)
            ToolResultMessage(
              id = toolCall.id,
              toolName = toolCall.name,
              text =
                result match
                  case s: String =>
                    s
                  case other =>
                    MessageCodec.toJson(other)
            )
          catch
            case e: Exception =>
              error(s"Tool execution failed: ${toolCall.name}", e)
              ToolResultMessage(
                id = toolCall.id,
                toolName = toolCall.name,
                text = s"""{"error": "${e.getMessage}"}"""
              )
        }
      case None =>
        debug(s"Tool not found: ${toolCall.name}")
        Rx.single(
          ToolResultMessage(
            id = toolCall.id,
            toolName = toolCall.name,
            text = s"""{"error": "Tool not found: ${toolCall.name}"}"""
          )
        )

  override def availableTools: Seq[ToolSpec] = toolSpecs

  /**
    * Clear all registered tools.
    *
    * @return
    *   A new empty executor
    */
  def clear(): LocalToolExecutor = new LocalToolExecutor()

end LocalToolExecutor

/**
  * A function that implements a tool. Takes arguments as a map and returns a result. The result can
  * be a String or any object that can be serialized to JSON.
  */
type ToolFunction = Map[String, Any] => Any

/**
  * Companion object for creating LocalToolExecutor instances.
  */
object LocalToolExecutor:
  def apply(): LocalToolExecutor = new LocalToolExecutor()

  /**
    * Create a LocalToolExecutor with pre-registered tools.
    *
    * @param tools
    *   Sequence of (ToolSpec, ToolFunction) pairs
    * @return
    *   A new LocalToolExecutor with the tools registered
    */
  def apply(tools: (ToolSpec, ToolFunction)*): LocalToolExecutor = new LocalToolExecutor()
    .registerTools(tools)
