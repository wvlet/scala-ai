package wvlet.ai.agent.chat.genai

import wvlet.airspec.AirSpec
import wvlet.ai.agent.LLMAgent
import wvlet.ai.agent.chat.{ChatEvent, ChatMessage, ChatObserver, ChatResponse}
import wvlet.log.LogSupport

import scala.collection.mutable.ListBuffer

/**
 * Test for GenAIRunner
 * 
 * Note: This test requires valid Google API credentials to run.
 * Set the following environment variables:
 * - GOOGLE_API_KEY: Your Google API key
 * - GOOGLE_PROJECT_ID: Your Google Cloud project ID
 */
class GenAIRunnerTest extends AirSpec with LogSupport:
  // Skip tests if credentials are not available
  private val skipTests = sys.env.get("GOOGLE_API_KEY").isEmpty && sys.env.get("GOOGLE_APPLICATION_CREDENTIALS").isEmpty
  
  test("GenAIRunner should create a chat session"):
    if skipTests then
      skip("Skipping test because Google API credentials are not available")
    
    // Create a test agent
    val agent = LLMAgent(
      name = "Test Agent",
      description = "A test agent for Google GenAI",
      model = GenAIModels.Gemini.Gemini1_5Flash,
      systemPrompt = Some("You are a helpful assistant.")
    )
    
    // Create a runner with config from environment
    val config = GenAIConfig.fromEnv()
    val runner = new GenAIRunner(agent, config)
    
    try
      // Create a chat session
      val session = runner.newChatSession
      
      // Create a test observer to collect responses
      val responses = ListBuffer[String]()
      val testObserver = new ChatObserver:
        override def onPartialResponse(event: ChatEvent): Unit =
          event match
            case ChatEvent.PartialResponse(text) =>
              responses += text
            case _ => // Ignore other events
        
        override def onComplete(response: ChatResponse): Unit =
          info(s"Chat completed with ${response.messages.size} messages")
        
        override def onError(e: Throwable): Unit =
          error(s"Error in chat: ${e.getMessage}", e)
      
      // Send a simple message
      val response = session.chat("Hello, how are you?", testObserver)
      
      // Verify response
      assert(response.messages.nonEmpty)
      assert(responses.nonEmpty)
      
      // Print the response for debugging
      info(s"Response: ${response.messages.last.text}")
    finally
      runner.close()
  
  test("GenAIRunner should handle tool calls"):
    if skipTests then
      skip("Skipping test because Google API credentials are not available")
    
    // Define a simple calculator tool
    import wvlet.ai.agent.chat.ToolSpec
    import wvlet.ai.agent.chat.ToolParameter
    import wvlet.ai.core.DataType
    
    val calculatorTool = ToolSpec(
      name = "calculator",
      description = "A simple calculator that can add, subtract, multiply, and divide numbers",
      parameters = List(
        ToolParameter(
          name = "operation",
          description = "The operation to perform (add, subtract, multiply, divide)",
          dataType = DataType.StringType
        ),
        ToolParameter(
          name = "a",
          description = "The first number",
          dataType = DataType.FloatType
        ),
        ToolParameter(
          name = "b",
          description = "The second number",
          dataType = DataType.FloatType
        )
      ),
      returnType = DataType.FloatType
    )
    
    // Create a test agent with the calculator tool
    val agent = LLMAgent(
      name = "Calculator Agent",
      description = "An agent that can perform calculations",
      model = GenAIModels.Gemini.Gemini1_5Flash,
      systemPrompt = Some("You are a helpful assistant that can perform calculations."),
      tools = List(calculatorTool)
    )
    
    // Create a runner with config from environment
    val config = GenAIConfig.fromEnv()
    val runner = new GenAIRunner(agent, config)
    
    try
      // Create a chat session
      val session = runner.newChatSession
      
      // Create a test observer to collect responses
      val responses = ListBuffer[String]()
      val testObserver = new ChatObserver:
        override def onPartialResponse(event: ChatEvent): Unit =
          event match
            case ChatEvent.PartialResponse(text) =>
              responses += text
            case ChatEvent.PartialToolRequestResponse(text) =>
              info(s"Tool request: $text")
            case _ => // Ignore other events
        
        override def onComplete(response: ChatResponse): Unit =
          info(s"Chat completed with ${response.messages.size} messages")
        
        override def onError(e: Throwable): Unit =
          error(s"Error in chat: ${e.getMessage}", e)
      
      // Send a message that should trigger a tool call
      val response = session.chat("What is 123 + 456?", testObserver)
      
      // Verify response
      assert(response.messages.nonEmpty)
      
      // Check if there was a tool call
      val lastMessage = response.messages.last
      lastMessage match
        case m: ChatMessage.AIMessage =>
          info(s"Tool calls: ${m.toolCalls.size}")
          if m.toolCalls.nonEmpty then
            val toolCall = m.toolCalls.head
            info(s"Tool call: ${toolCall.name}(${toolCall.args})")
        case _ =>
          info("No tool calls in the response")
      
      // Print the response for debugging
      info(s"Response: ${response.messages.last.text}")
    finally
      runner.close()