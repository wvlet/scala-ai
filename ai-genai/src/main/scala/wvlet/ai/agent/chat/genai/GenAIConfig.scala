package wvlet.ai.agent.chat.genai

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Configuration for Google GenAI client
 *
 * @param apiKey Google API key for authentication (if not using default credentials)
 * @param projectId Google Cloud project ID
 * @param location Google Cloud location (e.g., "us-central1")
 * @param useDefaultCredentials Whether to use default Google Cloud credentials
 */
case class GenAIConfig(
    apiKey: Option[String] = None,
    projectId: String = "",
    location: String = "us-central1",
    useDefaultCredentials: Boolean = true
):
  /**
   * Load configuration from application.conf
   */
  def withConfigFile(): GenAIConfig =
    val config = ConfigFactory.load()
    if config.hasPath("google.genai") then
      val genaiConfig = config.getConfig("google.genai")
      copy(
        apiKey = if genaiConfig.hasPath("api-key") then Some(genaiConfig.getString("api-key")) else None,
        projectId = if genaiConfig.hasPath("project-id") then genaiConfig.getString("project-id") else projectId,
        location = if genaiConfig.hasPath("location") then genaiConfig.getString("location") else location,
        useDefaultCredentials = if genaiConfig.hasPath("use-default-credentials") then 
          genaiConfig.getBoolean("use-default-credentials") 
        else 
          useDefaultCredentials
      )
    else
      this

  /**
   * Set the API key
   */
  def withApiKey(key: String): GenAIConfig = copy(apiKey = Some(key))

  /**
   * Set the project ID
   */
  def withProjectId(id: String): GenAIConfig = copy(projectId = id)

  /**
   * Set the location
   */
  def withLocation(loc: String): GenAIConfig = copy(location = loc)

  /**
   * Use default Google Cloud credentials
   */
  def withDefaultCredentials(use: Boolean): GenAIConfig = copy(useDefaultCredentials = use)

object GenAIConfig:
  /**
   * Create a configuration from environment variables
   */
  def fromEnv(): GenAIConfig =
    val apiKey = sys.env.get("GOOGLE_API_KEY")
    val projectId = sys.env.getOrElse("GOOGLE_PROJECT_ID", "")
    val location = sys.env.getOrElse("GOOGLE_LOCATION", "us-central1")
    val useDefaultCreds = sys.env.get("GOOGLE_USE_DEFAULT_CREDENTIALS") match
      case Some(value) => value.toLowerCase == "true"
      case None => true
    
    GenAIConfig(
      apiKey = apiKey,
      projectId = projectId,
      location = location,
      useDefaultCredentials = useDefaultCreds
    )