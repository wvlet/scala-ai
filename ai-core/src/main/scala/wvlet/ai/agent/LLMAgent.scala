package wvlet.ai.agent

import wvlet.ai.agent.chat.{ChatModel, ChatRequest, TextChatResponse, ToolSpec}

case class LLMAgent(
    // Name of this agent
    name: String,
    // Description of this agent's purpose and capabilities
    description: String,
    // LLM Model to use for this agent
    model: LLM,
    // System prompt defining the agent's behavior, instructions, and persona
    systemPrompt: String = "",
    // List of tools (functions) the agent is allowed to call
    tools: List[ToolSpec] = Nil,
    // Additional model configuration parameters (e.g., temperature, top-p, max output tokens)
    modelConfig: ModelConfig = ModelConfig()
) extends ChatModel:
  def withName(newName: String): LLMAgent               = this.copy(name = newName)
  def withDescription(newDescription: String): LLMAgent = this.copy(description = newDescription)
  def withModel(newModel: LLM): LLMAgent                = this.copy(model = newModel)
  def withSystemPrompt(newSystemPrompt: String): LLMAgent = this.copy(systemPrompt =
    newSystemPrompt
  )

  def withTools(newTools: List[ToolSpec]): LLMAgent = this.copy(tools = newTools)
  def withModelConfig(updater: ModelConfig => ModelConfig): LLMAgent = this.copy(modelConfig =
    updater(modelConfig)
  )

  /**
    * Process a chat request and generate a response.
    *
    * @param request
    *   The chat request containing the user's message and optional configuration overrides
    * @return
    *   A ChatResponse containing the LLM's response
    * @throws IllegalArgumentException
    *   if the request is invalid
    * @throws RuntimeException
    *   if there's an error processing the request
    */
  def runChat(request: ChatRequest): TextChatResponse =
    // Validate the request
    if request == null then
      throw new IllegalArgumentException("Chat request cannot be null")

    if request.message == null || request.message.trim.isEmpty then
      throw new IllegalArgumentException("Chat message cannot be null or empty")

    try
      // Merge the agent's model config with any overrides in the request
      val effectiveConfig =
        request.overrideConfig match
          case Some(overrideConfig) =>
            modelConfig.overrideWith(overrideConfig)
          case None =>
            modelConfig

      // Here we would typically call the LLM API with the request and configuration
      // Since the actual implementation depends on the specific LLM API being used,
      // this is a placeholder implementation

      // For a real implementation, we would:
      // 1. Format the system prompt, user message, and any context for the LLM
      // 2. Call the LLM API with the formatted prompt and effective configuration
      // 3. Process the LLM's response and return it as a TextChatResponse

      // For now, we'll just return a placeholder response
      TextChatResponse(s"Response from ${model.name} to message: ${request.message}")
    catch
      case e: Exception =>
        // Log the error
        System.err.println(s"Error processing chat request: ${e.getMessage}")
        // Rethrow as a RuntimeException
        throw new RuntimeException(s"Error processing chat request: ${e.getMessage}", e)

  end runChat

  /**
    * Process a chat request as required by the ChatModel interface. This implementation delegates
    * to runChat and prints the response.
    *
    * @param request
    *   The chat request containing the user's message and optional configuration overrides
    * @return
    *   Unit (as defined by the ChatModel interface)
    */
  override def chat(request: ChatRequest): Unit =
    try
      val response = runChat(request)
      println(s"Chat response: ${response.text}")
    catch
      case e: Exception =>
        System.err.println(s"Error in chat: ${e.getMessage}")
      // In a real implementation, we might want to handle the error differently
      // For example, we might want to return an error response to the user

end LLMAgent
