package wvlet.ai.agent.mcp

import wvlet.airframe.rx.Rx
import wvlet.airframe.codec.MessageCodec
import wvlet.ai.agent.mcp.MCPMessages.*
import wvlet.log.LogSupport
import java.io.{BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter}
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.{Promise, ExecutionContext}
import scala.jdk.CollectionConverters.*
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * MCP client that communicates with servers via stdio (stdin/stdout).
  *
  * @param command
  *   Command to start the MCP server process
  * @param args
  *   Arguments for the command
  * @param env
  *   Environment variables for the process
  * @param workingDir
  *   Working directory for the process
  */
class StdioMCPClient(
    command: String,
    args: Seq[String] = Seq.empty,
    env: Map[String, String] = Map.empty,
    workingDir: Option[String] = None
) extends MCPClient
    with LogSupport:

  private var process: Option[Process]       = None
  private var reader: Option[BufferedReader] = None
  private var writer: Option[BufferedWriter] = None
  private val connected                      = AtomicBoolean(false)
  private val pendingRequests              = ConcurrentHashMap[String, Promise[JsonRpc.Response]]()
  private var readerThread: Option[Thread] = None

  /**
    * Start the MCP server process and establish communication.
    */
  private def startProcess(): Unit =
    if process.isEmpty then
      debug(s"Starting MCP server: $command ${args.mkString(" ")}")

      val pb = ProcessBuilder((command +: args).asJava)
      workingDir.foreach(dir => pb.directory(java.io.File(dir)))

      // Add environment variables
      if env.nonEmpty then
        val processEnv = pb.environment()
        env.foreach { case (k, v) =>
          processEnv.put(k, v)
        }

      val proc = pb.start()
      process = Some(proc)

      // Set up readers and writers
      reader = Some(BufferedReader(InputStreamReader(proc.getInputStream)))
      writer = Some(BufferedWriter(OutputStreamWriter(proc.getOutputStream)))

      // Start reader thread to handle responses
      readerThread = Some(Thread(() => readResponses()))
      readerThread.foreach(_.start())

      connected.set(true)
      debug("MCP server process started")

  /**
    * Read responses from the server in a separate thread.
    */
  private def readResponses(): Unit =
    try
      reader.foreach { r =>
        var line = r.readLine()
        while line != null && connected.get() do
          debug(s"Received from server: $line")
          handleResponse(line)
          line = r.readLine()
      }
    catch
      case e: Exception =>
        if connected.get() then
          error(s"Error reading from MCP server", e)
    finally
      connected.set(false)

  /**
    * Handle a response line from the server.
    */
  private def handleResponse(line: String): Unit =
    try
      JsonRpc.parse(line) match
        case response: JsonRpc.Response =>
          response.id match
            case Some(id) =>
              val idStr = id.toString
              Option(pendingRequests.remove(idStr)).foreach { promise =>
                promise.success(response)
              }
            case None =>
              warn(s"Received response without id: $line")

        case notification: JsonRpc.Notification =>
          debug(s"Received notification: ${notification.method}")
          // Handle notifications if needed

        case request: JsonRpc.Request =>
          warn(s"Received unexpected request from server: ${request.method}")
    catch
      case e: Exception =>
        error(s"Failed to parse response: ${e.getMessage}", e)

  override def initialize(): Rx[InitializeResult] =
    if !connected.get() then
      startProcess()

    val request = MCPMessages.createInitializeRequest("scala-ai", "1.0.0")
    sendRequest(request).map { response =>
      response.result match
        case Some(result: Map[?, ?]) =>
          val codec = MessageCodec.of[InitializeResult]
          codec.fromMap(result.asInstanceOf[Map[String, Any]])
        case Some(other) =>
          throw new RuntimeException(
            s"Initialize failed: unexpected result type. Expected a JSON object, but got $other"
          )
        case None =>
          throw new RuntimeException(s"Initialize failed: ${response.error}")
    }

  override def listTools(): Rx[ListToolsResult] =
    val request = MCPMessages.createListToolsRequest()
    sendRequest(request).map { response =>
      response.result match
        case Some(result: Map[?, ?]) =>
          val codec = MessageCodec.of[ListToolsResult]
          codec.fromMap(result.asInstanceOf[Map[String, Any]])
        case Some(other) =>
          throw new RuntimeException(
            s"List tools failed: unexpected result type. Expected a JSON object, but got $other"
          )
        case None =>
          throw new RuntimeException(s"List tools failed: ${response.error}")
    }

  override def callTool(toolName: String, arguments: Map[String, Any]): Rx[CallToolResult] =
    val request = MCPMessages.createCallToolRequest(toolName, arguments)
    sendRequest(request).map { response =>
      response.result match
        case Some(result: Map[?, ?]) =>
          val codec = MessageCodec.of[CallToolResult]
          codec.fromMap(result.asInstanceOf[Map[String, Any]])
        case Some(other) =>
          throw new RuntimeException(
            s"Tool call failed: unexpected result type. Expected a JSON object, but got $other"
          )
        case None =>
          throw new RuntimeException(s"Tool call failed: ${response.error}")
    }

  override def sendRequest(request: JsonRpc.Request): Rx[JsonRpc.Response] =
    if !connected.get() then
      throw new IllegalStateException("MCP client not connected")

    val promise = Promise[JsonRpc.Response]()

    request.id match
      case Some(id) =>
        pendingRequests.put(id.toString, promise)

        // Send the request
        val json = MessageCodec.toJson(request)
        debug(s"Sending request: $json")

        writer.foreach { w =>
          w.write(json)
          w.newLine()
          w.flush()
        }

        // Convert Promise to Rx
        Rx.fromFuture(promise.future)
          .recover { case e: Exception =>
            pendingRequests.remove(id.toString)
            JsonRpc.Response(
              id = request.id,
              error = Some(
                JsonRpc.ErrorObject(
                  JsonRpc.ErrorCode.InternalError,
                  s"Request failed: ${e.getMessage}"
                )
              )
            )
          }

      case None =>
        Rx.single(
          JsonRpc.Response(
            id = None,
            error = Some(
              JsonRpc.ErrorObject(JsonRpc.ErrorCode.InvalidRequest, "Request must have an id")
            )
          )
        )

    end match

  end sendRequest

  override def close(): Unit =
    if connected.get() then
      connected.set(false)

      // Close streams
      writer.foreach(_.close())
      reader.foreach(_.close())

      // Terminate process
      process.foreach { p =>
        p.destroy()
        if !p.waitFor(5, TimeUnit.SECONDS) then
          p.destroyForcibly()
      }

      // Stop reader thread
      readerThread.foreach(_.interrupt())

      // Clear pending requests
      pendingRequests
        .values()
        .asScala
        .foreach { promise =>
          promise.failure(new RuntimeException("Client closed"))
        }
      pendingRequests.clear()

      debug("MCP client closed")

  override def isConnected: Boolean = connected.get()

end StdioMCPClient
