package wvlet.ai.agent.chat.genai.examples

import wvlet.ai.agent.LLMAgent
import wvlet.ai.agent.chat.{ChatEvent, ChatObserver, ChatResponse}
import wvlet.ai.agent.chat.genai.{GenAIConfig, GenAIModels, GenAIRunner}
import wvlet.log.{LogLevel, LogSupport, Logger}

import scala.io.StdIn

/**
 * Example application for chatting with Google GenAI models
 */
object GenAIChatExample extends App with LogSupport:
  // Set up logging
  Logger.setDefaultLogLevel(LogLevel.INFO)
  
  // Parse command line arguments
  val modelName = args.headOption.getOrElse("gemini-1.5-flash")
  
  // Get the model
  val model = modelName match
    case "gemini-1.0-pro" => GenAIModels.Gemini.Gemini1_0Pro
    case "gemini-1.0-pro-vision" => GenAIModels.Gemini.Gemini1_0ProVision
    case "gemini-1.0-ultra" => GenAIModels.Gemini.Gemini1_0Ultra
    case "gemini-1.5-pro" => GenAIModels.Gemini.Gemini1_5Pro
    case "gemini-1.5-flash" => GenAIModels.Gemini.Gemini1_5Flash
    case other => 
      info(s"Unknown model: $other, using Gemini 1.5 Flash")
      GenAIModels.Gemini.Gemini1_5Flash
  
  // Create the agent
  val agent = LLMAgent(
    name = "Chat Assistant",
    description = "A helpful chat assistant powered by Google Gemini",
    model = model,
    systemPrompt = Some("You are a helpful, accurate, and friendly AI assistant.")
  )
  
  // Load configuration from environment or config file
  val config = GenAIConfig.fromEnv().withConfigFile()
  
  // Create the runner
  val runner = new GenAIRunner(agent, config)
  
  try
    // Create a chat session
    val session = runner.newChatSession
    
    // Create a streaming observer
    val streamingObserver = new ChatObserver:
      override def onPartialResponse(event: ChatEvent): Unit =
        event match
          case ChatEvent.PartialResponse(text) =>
            print(text)
            System.out.flush()
          case _ => // Ignore other events
      
      override def onComplete(response: ChatResponse): Unit =
        println("\n")
      
      override def onError(e: Throwable): Unit =
        error(s"Error: ${e.getMessage}")
    
    // Start the chat loop
    println(s"Chat with ${model.name} (type 'exit' to quit)")
    println("----------------------------------------------")
    
    var chatResponse: Option[ChatResponse] = None
    
    while true do
      print("> ")
      val input = StdIn.readLine()
      
      if input.toLowerCase == "exit" then
        println("Goodbye!")
        return
      
      print("AI: ")
      
      // Continue the conversation if we have a previous response
      chatResponse match
        case Some(prevResponse) =>
          chatResponse = Some(session.continueChat(prevResponse, input, streamingObserver))
        case None =>
          chatResponse = Some(session.chat(input, streamingObserver))
  finally
    runner.close()

/**
 * Example of using the GenAIRunner programmatically
 */
object GenAIProgrammaticExample extends LogSupport:
  def main(args: Array[String]): Unit =
    // Set up logging
    Logger.setDefaultLogLevel(LogLevel.INFO)
    
    // Create the agent
    val agent = LLMAgent(
      name = "Code Assistant",
      description = "A helpful coding assistant powered by Google Gemini",
      model = GenAIModels.Gemini.Gemini1_5Pro,
      systemPrompt = Some(
        """You are an expert programming assistant.
          |When asked about code, provide clear, efficient, and well-commented solutions.
          |Explain your reasoning and any important concepts.
          |""".stripMargin
      )
    )
    
    // Load configuration
    val config = GenAIConfig.fromEnv().withConfigFile()
    
    // Create the runner
    val runner = new GenAIRunner(agent, config)
    
    try
      // Create a chat session
      val session = runner.newChatSession
      
      // Send a message and get a response
      val response = session.chat("Write a Scala function to calculate the Fibonacci sequence recursively with memoization.")
      
      // Print the response
      info(s"Response: ${response.messages.last.text}")
      
      // Print stats
      info(s"Input tokens: ${response.stats.inputTokens}")
      info(s"Output tokens: ${response.stats.outputTokens}")
      info(s"Total tokens: ${response.stats.totalTokens}")
      info(s"Latency: ${response.stats.latencyMs}ms")
    finally
      runner.close()