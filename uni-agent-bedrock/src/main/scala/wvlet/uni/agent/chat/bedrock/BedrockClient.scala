package wvlet.uni.agent.chat.bedrock

import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponseHandler
import wvlet.uni.agent.chat.ChatObserver
import wvlet.uni.agent.chat.ChatRequest

import java.util.concurrent.CompletableFuture

trait BedrockClient extends AutoCloseable:
  def converseStream(
      request: ConverseStreamRequest,
      handler: ConverseStreamResponseHandler
  ): CompletableFuture[Void]

object BedrockClient:
  def apply(config: BedrockConfig): BedrockClient = BedrockAsyncClient(config)
  def apply(): BedrockClient                      = apply(BedrockConfig())

class BedrockAsyncClient(config: BedrockConfig) extends BedrockClient:
  import wvlet.uni.agent.core.ops.*

  private val client = BedrockRuntimeAsyncClient
    .builder()
    .region(config.region)
    .credentialsProvider(config.credentialProvider)
    .pipe(config.asyncClientConfig(_))
    .build()

  override def converseStream(
      request: ConverseStreamRequest,
      handler: ConverseStreamResponseHandler
  ): CompletableFuture[Void] = client.converseStream(request, handler)

  override def close(): Unit = client.close()
