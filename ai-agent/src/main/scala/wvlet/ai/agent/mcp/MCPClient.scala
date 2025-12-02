package wvlet.ai.agent.mcp

import wvlet.airframe.rx.Rx
import wvlet.ai.agent.mcp.MCPMessages.*

/**
  * Client interface for communicating with MCP servers.
  */
trait MCPClient:
  /**
    * Initialize the connection with the MCP server.
    *
    * @return
    *   Server capabilities and information
    */
  def initialize(): Rx[InitializeResult]

  /**
    * List available tools from the MCP server.
    *
    * @return
    *   List of available tools
    */
  def listTools(): Rx[ListToolsResult]

  /**
    * Call a tool on the MCP server.
    *
    * @param toolName
    *   Name of the tool to call
    * @param arguments
    *   Arguments to pass to the tool
    * @return
    *   Tool execution result
    */
  def callTool(toolName: String, arguments: Map[String, Any]): Rx[CallToolResult]

  /**
    * Send a raw JSON-RPC request to the server.
    *
    * @param request
    *   The JSON-RPC request
    * @return
    *   The JSON-RPC response
    */
  def sendRequest(request: JsonRpc.Request): Rx[JsonRpc.Response]

  /**
    * Close the connection to the MCP server.
    */
  def close(): Unit

  /**
    * Check if the client is connected to the server.
    *
    * @return
    *   true if connected, false otherwise
    */
  def isConnected: Boolean

end MCPClient
