package wvlet.ai.agent.chat.bedrock

import software.amazon.awssdk.auth.credentials.{AwsCredentialsProvider, DefaultCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.model.{
  ConverseStreamRequest,
  InferenceConfiguration
}
import software.amazon.awssdk.services.bedrockruntime.{
  BedrockRuntimeAsyncClient,
  BedrockRuntimeAsyncClientBuilder
}
import wvlet.ai.agent.LLMAgent
import wvlet.ai.agent.chat.{ChatModel, ChatObserver, ChatRequest}
import wvlet.log.LogSupport

import scala.jdk.CollectionConverters.*

case class BedrockConfig(
    region: Region = Region.US_EAST_1,
    credentialProvider: AwsCredentialsProvider = DefaultCredentialsProvider.create(),
    asyncClientConfig: BedrockRuntimeAsyncClientBuilder => BedrockRuntimeAsyncClientBuilder =
      identity
)

class BedrockChat(agent: LLMAgent, config: BedrockConfig) extends ChatModel with LogSupport:

  import wvlet.ai.core.ChainingUtil.*

  private val client = BedrockRuntimeAsyncClient
    .builder()
    .region(config.region)
    .credentialsProvider(config.credentialProvider)
    .pipe(config.asyncClientConfig(_))
    .build()

  override def chat(request: ChatRequest): Unit                               = ???
  override def chatStream(request: ChatRequest, observer: ChatObserver): Unit = ???

  private[bedrock] def newConverseRequest(request: ChatRequest): ConverseStreamRequest =

    val builder = ConverseStreamRequest.builder().modelId(agent.model.id)

    // Set inference configuration
    val inferenceConfig = InferenceConfiguration
      .builder()
      .ifDefined(agent.modelConfig.maxOutputTokens) { (builder, maxTokens) =>
        builder.maxTokens(maxTokens.toInt)
      }
      .ifDefined(agent.modelConfig.temperature) { (builder, temperature) =>
        builder.temperature(temperature.toFloat)
      }
      .ifDefined(agent.modelConfig.topP) { (builder, topP) =>
        builder.topP(topP.toFloat)
      }
      .ifDefined(agent.modelConfig.topK) { (builder, topK) =>
        warn(s"top-k parameter is not supported in Bedrock")
        builder
      }
      .ifDefined(agent.modelConfig.stopSequences) { (builder, stopSequences) =>
        builder.stopSequences(stopSequences.asJava)
      }
      .build()

    builder.inferenceConfig(inferenceConfig)

    // Reasoning config
    builder.ifDefined(agent.modelConfig.reasoningConfig) { (builder, config) =>
      builder.additionalModelRequestFields(
        DocumentUtil.fromMap(
          Map("thinking" -> Map("type" -> "enabled", "budget_tokens" -> config.reasoningBudget))
        )
      )
    }

    builder.build()

  end newConverseRequest

end BedrockChat
