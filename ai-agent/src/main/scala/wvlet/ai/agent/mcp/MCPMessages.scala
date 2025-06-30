package wvlet.ai.agent.mcp

/**
  * MCP-specific message types that extend JSON-RPC protocol.
  *
  * Based on: https://modelcontextprotocol.io/specification/basic/messages/
  */
object MCPMessages:

  /**
    * MCP protocol version
    */
  val PROTOCOL_VERSION = "2024-11-05"

  /**
    * Client capabilities
    */
  case class ClientCapabilities(
      experimental: Option[Map[String, Any]] = None,
      sampling: Option[Map[String, Any]] = None
  )

  /**
    * Server capabilities
    */
  case class ServerCapabilities(
      experimental: Option[Map[String, Any]] = None,
      logging: Option[Map[String, Any]] = None,
      prompts: Option[PromptsCapability] = None,
      resources: Option[ResourcesCapability] = None,
      tools: Option[ToolsCapability] = None
  )

  case class PromptsCapability(listChanged: Option[Boolean] = None)
  case class ResourcesCapability(
      subscribe: Option[Boolean] = None,
      listChanged: Option[Boolean] = None
  )

  case class ToolsCapability(listChanged: Option[Boolean] = None)

  /**
    * Implementation details
    */
  case class Implementation(name: String, version: String)

  /**
    * Initialize request parameters
    */
  case class InitializeParams(
      protocolVersion: String = PROTOCOL_VERSION,
      capabilities: ClientCapabilities,
      clientInfo: Implementation
  )

  /**
    * Initialize result
    */
  case class InitializeResult(
      protocolVersion: String = PROTOCOL_VERSION,
      capabilities: ServerCapabilities,
      serverInfo: Implementation
  )

  /**
    * Tool definition from MCP server
    */
  case class MCPTool(
      name: String,
      description: Option[String] = None,
      inputSchema: Map[String, Any]
  )

  /**
    * List tools result
    */
  case class ListToolsResult(tools: Seq[MCPTool])

  /**
    * Tool call request
    */
  case class CallToolParams(name: String, arguments: Option[Map[String, Any]] = None)

  /**
    * Tool call result
    */
  case class CallToolResult(content: Seq[Map[String, Any]], isError: Option[Boolean] = None)

  /**
    * Content types for tool results
    */
  sealed trait ToolResultContent
  case class TextContent(`type`: String = "text", text: String) extends ToolResultContent
  case class ImageContent(`type`: String = "image", data: String, mimeType: String)
      extends ToolResultContent

  case class ResourceContent(`type`: String = "resource", resource: ResourceReference)
      extends ToolResultContent

  /**
    * Resource reference
    */
  case class ResourceReference(uri: String, mimeType: Option[String] = None)

  /**
    * Create an initialize request
    */
  def createInitializeRequest(clientName: String, clientVersion: String): JsonRpc.Request = JsonRpc
    .Request(
      id = Some("init"),
      method = "initialize",
      params = Some(
        Map(
          "protocolVersion" -> PROTOCOL_VERSION,
          "capabilities"    -> Map.empty[String, Any],
          "clientInfo"      -> Map("name" -> clientName, "version" -> clientVersion)
        )
      )
    )

  /**
    * Create a list tools request
    */
  def createListToolsRequest(): JsonRpc.Request = JsonRpc.Request(
    id = Some("list-tools"),
    method = "tools/list"
  )

  /**
    * Create a tool call request
    */
  def createCallToolRequest(toolName: String, arguments: Map[String, Any]): JsonRpc.Request =
    JsonRpc.Request(
      id = Some(s"call-$toolName-${java.util.UUID.randomUUID()}"),
      method = "tools/call",
      params = Some(Map("name" -> toolName, "arguments" -> arguments))
    )

end MCPMessages
