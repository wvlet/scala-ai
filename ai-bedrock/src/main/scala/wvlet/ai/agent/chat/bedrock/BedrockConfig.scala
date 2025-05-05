package wvlet.ai.agent.chat.bedrock

import software.amazon.awssdk.auth.credentials.{AwsCredentialsProvider, DefaultCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClientBuilder

case class BedrockConfig(
    region: Region = Region.US_EAST_1,
    credentialProvider: AwsCredentialsProvider = DefaultCredentialsProvider.create(),
    asyncClientConfig: BedrockRuntimeAsyncClientBuilder => BedrockRuntimeAsyncClientBuilder =
      identity
):
  def withRegion(region: Region): BedrockConfig = this.copy(region = region)

  def withCredentials(credentialProvider: AwsCredentialsProvider): BedrockConfig = this.copy(
    credentialProvider = credentialProvider
  )

  def withAsyncClientConfig(
      config: BedrockRuntimeAsyncClientBuilder => BedrockRuntimeAsyncClientBuilder
  ): BedrockConfig = this.copy(asyncClientConfig = config)
