package wvlet.ai.agent.chat.genai

import wvlet.ai.agent.LLM

/**
 * Google GenAI model definitions
 */
object GenAIModels:
  /**
   * Google Gemini models
   */
  object Gemini:
    // Gemini 1.0 models
    val Gemini1_0Pro = LLM("gemini-1.0-pro", "Gemini 1.0 Pro")
    val Gemini1_0ProVision = LLM("gemini-1.0-pro-vision", "Gemini 1.0 Pro Vision")
    val Gemini1_0Ultra = LLM("gemini-1.0-ultra", "Gemini 1.0 Ultra")
    val Gemini1_0UltraVision = LLM("gemini-1.0-ultra-vision", "Gemini 1.0 Ultra Vision")
    
    // Gemini 1.5 models
    val Gemini1_5Pro = LLM("gemini-1.5-pro", "Gemini 1.5 Pro")
    val Gemini1_5ProLatest = LLM("gemini-1.5-pro-latest", "Gemini 1.5 Pro Latest")
    val Gemini1_5Flash = LLM("gemini-1.5-flash", "Gemini 1.5 Flash")
    val Gemini1_5FlashLatest = LLM("gemini-1.5-flash-latest", "Gemini 1.5 Flash Latest")
    
    // Gemini 1.5 preview models
    val Gemini1_5Pro8k = LLM("gemini-1.5-pro-preview-0409", "Gemini 1.5 Pro Preview (8K)")
    val Gemini1_5Pro32k = LLM("gemini-1.5-pro-32k-preview-0409", "Gemini 1.5 Pro Preview (32K)")
    val Gemini1_5Pro128k = LLM("gemini-1.5-pro-128k-preview-0409", "Gemini 1.5 Pro Preview (128K)")
    
    // Function for getting the latest model version
    def getLatestModel(modelName: String): String =
      modelName match
        case "gemini-1.5-pro" => "gemini-1.5-pro-latest"
        case "gemini-1.5-flash" => "gemini-1.5-flash-latest"
        case other => other
  
  /**
   * Google PaLM models (legacy)
   */
  object PaLM:
    val TextBison = LLM("text-bison", "PaLM 2 Text Bison")
    val ChatBison = LLM("chat-bison", "PaLM 2 Chat Bison")
    val CodeBison = LLM("code-bison", "PaLM 2 Code Bison")
    val CodeChat = LLM("codechat-bison", "PaLM 2 Code Chat")