package wvlet.ai.agent.chat.genai

import com.google.cloud.ai.generativelanguage.v1beta.{
  GenerateContentRequest,
  GenerateContentResponse,
  GenerationConfig,
  GenerationEndpoint,
  GenerationServiceClient,
  GenerationServiceSettings,
  Part,
  Tool => GoogleTool,
  ToolConfig,
  FunctionDeclaration,
  Schema,
  Type => GoogleType
}
import com.google.cloud.ai.generativelanguage.v1beta.Content.Role
import com.google.api.gax.core.{FixedCredentialsProvider, NoCredentialsProvider}
import com.google.api.gax.rpc.{ClientStream, ResponseObserver, StreamController}
import com.google.auth.oauth2.{GoogleCredentials, ServiceAccountCredentials}
import wvlet.ai.agent.chat.{ChatRequest, ToolSpec}
import wvlet.log.LogSupport

import java.util.concurrent.{CompletableFuture, ExecutorService, Executors}
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

/**
 * Client for interacting with Google GenAI API
 */
class GenAIClient(config: GenAIConfig) extends AutoCloseable with LogSupport:
  private val client: GenerationServiceClient = createClient()
  private val executor: ExecutorService = Executors.newCachedThreadPool()

  /**
   * Create a Google GenAI client
   */
  private def createClient(): GenerationServiceClient =
    val settingsBuilder = GenerationServiceSettings.newBuilder()
    
    // Configure authentication
    if config.useDefaultCredentials then
      // Use default credentials
      debug("Using default Google Cloud credentials")
      settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(GoogleCredentials.getApplicationDefault()))
    else if config.apiKey.isDefined then
      // Use API key
      debug("Using API key for authentication")
      settingsBuilder.setCredentialsProvider(NoCredentialsProvider.create())
      settingsBuilder.setEndpoint(s"generativelanguage.googleapis.com:443")
      settingsBuilder.setQuotaProjectId(config.projectId)
    else
      throw new IllegalArgumentException("Either useDefaultCredentials must be true or apiKey must be provided")
    
    GenerationServiceClient.create(settingsBuilder.build())

  /**
   * Generate content using the Google GenAI API
   */
  def generateContent(
      request: GenerateContentRequest
  ): CompletableFuture[GenerateContentResponse] =
    val future = new CompletableFuture[GenerateContentResponse]()
    try
      val response = client.generateContent(request)
      future.complete(response)
    catch
      case e: Exception =>
        future.completeExceptionally(e)
    future

  /**
   * Generate content using streaming
   */
  def generateContentStream(
      request: GenerateContentRequest,
      responseObserver: ResponseObserver[GenerateContentResponse]
  ): ClientStream[GenerateContentRequest] =
    client.generateContentStream(responseObserver)

  /**
   * Convert a ToolSpec to a Google FunctionDeclaration
   */
  def convertToolToFunctionDeclaration(tool: ToolSpec): FunctionDeclaration =
    val parametersBuilder = Schema.newBuilder()
    
    // Set type to OBJECT
    parametersBuilder.setType(GoogleType.OBJECT)
    
    // Add properties for each parameter
    val propertiesBuilder = parametersBuilder.getPropertiesBuilder()
    tool.parameters.foreach { param =>
      val schemaBuilder = Schema.newBuilder()
      
      // Set the type based on the parameter's data type
      import wvlet.ai.core.DataType.*
      param.dataType match
        case StringType => schemaBuilder.setType(GoogleType.STRING)
        case IntegerType => schemaBuilder.setType(GoogleType.INTEGER)
        case FloatType => schemaBuilder.setType(GoogleType.NUMBER)
        case BooleanType => schemaBuilder.setType(GoogleType.BOOLEAN)
        case ArrayType(elementType) => 
          schemaBuilder.setType(GoogleType.ARRAY)
          val itemsBuilder = schemaBuilder.getItemsBuilder()
          elementType match
            case StringType => itemsBuilder.setType(GoogleType.STRING)
            case IntegerType => itemsBuilder.setType(GoogleType.INTEGER)
            case FloatType => itemsBuilder.setType(GoogleType.NUMBER)
            case BooleanType => itemsBuilder.setType(GoogleType.BOOLEAN)
            case _ => itemsBuilder.setType(GoogleType.OBJECT)
        case _ => schemaBuilder.setType(GoogleType.OBJECT)
      
      // Set description if available
      if param.description.nonEmpty then
        schemaBuilder.setDescription(param.description)
      
      propertiesBuilder.putFields(param.name, schemaBuilder.build())
    }
    
    // Add required parameters
    val requiredParams = tool.parameters.filter(_.dataType.isRequired).map(_.name)
    parametersBuilder.addAllRequired(requiredParams.asJava)
    
    // Build the function declaration
    FunctionDeclaration.newBuilder()
      .setName(tool.name)
      .setDescription(tool.description)
      .setParameters(parametersBuilder.build())
      .build()

  override def close(): Unit =
    Try {
      client.close()
      executor.shutdown()
    } match
      case Success(_) => debug("GenAI client closed successfully")
      case Failure(e) => warn(s"Failed to close GenAI client: ${e.getMessage}")

object GenAIClient:
  def apply(config: GenAIConfig): GenAIClient = new GenAIClient(config)