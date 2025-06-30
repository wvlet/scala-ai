package wvlet.ai.agent.mcp

import wvlet.ai.agent.chat.{ChatMessage, ToolSpec, ToolParameter}
import wvlet.ai.agent.chat.ChatMessage.{ToolCallRequest, ToolResultMessage}
import wvlet.ai.agent.tool.ToolExecutor
import wvlet.ai.agent.core.DataType
import wvlet.airframe.rx.Rx
import wvlet.airframe.codec.MessageCodec
import wvlet.log.LogSupport

/**
  * ToolExecutor implementation that executes tools via MCP protocol.
  *
  * @param client
  *   The MCP client to use for communication
  */
class MCPToolExecutor(private val client: MCPClient) extends ToolExecutor with LogSupport:

  private var cachedTools: Seq[ToolSpec] = Seq.empty
  private var initialized: Boolean       = false

  /**
    * Initialize the MCP connection and discover available tools.
    */
  def initialize(): Rx[Unit] =
    if initialized then
      Rx.single(())
    else
      client
        .initialize()
        .flatMap { initResult =>
          debug(s"MCP server initialized: ${initResult.serverInfo}")

          // List available tools
          client
            .listTools()
            .map { toolsResult =>
              cachedTools = toolsResult.tools.map(convertMCPToolToSpec)
              initialized = true
              debug(s"Discovered ${cachedTools.size} tools from MCP server")
            }
        }

  override def executeToolCall(toolCall: ToolCallRequest): Rx[ToolResultMessage] =
    // Ensure we're initialized
    val initRx =
      if initialized then
        Rx.single(())
      else
        initialize()

    initRx.flatMap { _ =>
      debug(s"Executing MCP tool: ${toolCall.name}")

      client
        .callTool(toolCall.name, toolCall.args)
        .map { result =>
          // Convert MCP result to ToolResultMessage
          val text = result
            .content
            .map { contentMap =>
              contentMap.get("type") match
                case Some("text") =>
                  contentMap.getOrElse("text", "").toString
                case Some("image") =>
                  val data     = contentMap.getOrElse("data", "").toString
                  val mimeType = contentMap.getOrElse("mimeType", "").toString
                  s"""{"type": "image", "mimeType": "$mimeType", "data": "$data"}"""
                case Some("resource") =>
                  contentMap.get("resource") match
                    case Some(res: Map[String, Any]) =>
                      val uri = res.getOrElse("uri", "").toString
                      s"""{"type": "resource", "uri": "$uri"}"""
                    case _ =>
                      """{"type": "resource", "uri": ""}"""
                case _ =>
                  MessageCodec.toJson(contentMap)
            }
            .mkString("\n")

          ToolResultMessage(id = toolCall.id, toolName = toolCall.name, text = text)
        }
        .recover { case e: Exception =>
          error(s"MCP tool execution failed: ${e.getMessage}", e)
          ToolResultMessage(
            id = toolCall.id,
            toolName = toolCall.name,
            text = s"""{"error": "${e.getMessage}"}"""
          )
        }
    }

  end executeToolCall

  override def availableTools: Seq[ToolSpec] = cachedTools

  /**
    * Convert an MCP tool definition to a ToolSpec.
    */
  private def convertMCPToolToSpec(mcpTool: MCPMessages.MCPTool): ToolSpec =
    val parameters = extractParameters(mcpTool.inputSchema)

    ToolSpec(
      name = mcpTool.name,
      description = mcpTool.description.getOrElse(""),
      parameters = parameters,
      returnType = DataType.JsonType // MCP tools typically return JSON
    )

  /**
    * Extract parameters from JSON Schema.
    */
  private def extractParameters(schema: Map[String, Any]): List[ToolParameter] =
    schema.get("properties") match
      case Some(props: Map[String, Any]) =>
        val required =
          schema.get("required") match
            case Some(req: List[String]) =>
              req.toSet
            case _ =>
              Set.empty[String]

        props
          .map { case (name, propSchemaAny) =>
            val propSchema = propSchemaAny.asInstanceOf[Map[String, Any]]
            val description =
              propSchema.get("description") match
                case Some(desc: String) =>
                  desc
                case _ =>
                  ""

            val paramType: DataType =
              propSchema.get("type") match
                case Some(t: String) =>
                  t match
                    case "string" =>
                      DataType.StringType
                    case "number" =>
                      DataType.FloatType
                    case "integer" =>
                      DataType.IntegerType
                    case "boolean" =>
                      DataType.BooleanType
                    case "array" =>
                      DataType.ArrayType(DataType.AnyType)
                    case "object" =>
                      DataType.JsonType
                    case _ =>
                      DataType.JsonType
                case _ =>
                  DataType.JsonType

            val defaultValue = propSchema.get("default")

            ToolParameter(
              name = name,
              description = description,
              dataType = paramType,
              defaultValue = defaultValue
            )
          }
          .toList

      case _ =>
        List.empty

  /**
    * Close the MCP connection.
    */
  def close(): Unit =
    client.close()
    initialized = false
    cachedTools = Seq.empty

end MCPToolExecutor

/**
  * Factory for creating MCPToolExecutor instances.
  */
object MCPToolExecutor:
  /**
    * Create an MCPToolExecutor with a stdio-based MCP server.
    *
    * @param command
    *   Command to start the MCP server
    * @param args
    *   Arguments for the command
    * @param env
    *   Environment variables
    * @param workingDir
    *   Working directory
    * @return
    *   An Rx stream that emits the initialized executor
    */
  def fromCommand(
      command: String,
      args: Seq[String] = Seq.empty,
      env: Map[String, String] = Map.empty,
      workingDir: Option[String] = None
  ): Rx[MCPToolExecutor] =
    val client   = StdioMCPClient(command, args, env, workingDir)
    val executor = MCPToolExecutor(client)
    executor.initialize().map(_ => executor)

end MCPToolExecutor
