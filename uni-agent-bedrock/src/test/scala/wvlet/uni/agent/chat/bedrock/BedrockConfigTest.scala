package wvlet.uni.agent.chat.bedrock

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClientBuilder
import wvlet.uni.test.UniTest

class BedrockConfigTest extends UniTest:

  test("default values") {
    val config = BedrockConfig()
    config.region shouldBe Region.US_EAST_1
    config.credentialProvider shouldBe DefaultCredentialsProvider.create()

    // Verify the default asyncClientConfig is identity
    val mockBuilder = BedrockRuntimeAsyncClient.builder()
    config.asyncClientConfig(mockBuilder) shouldBeTheSameInstanceAs mockBuilder
  }

  test("withRegion") {
    val config    = BedrockConfig()
    val newConfig = config.withRegion(Region.US_WEST_2)
    newConfig.region shouldBe Region.US_WEST_2
    // Ensure other properties are unchanged
    newConfig.credentialProvider shouldBe config.credentialProvider
    newConfig.asyncClientConfig shouldBe config.asyncClientConfig
  }

  test("withCredentials") {
    val config = BedrockConfig()
    val creds  = StaticCredentialsProvider.create(
      AwsBasicCredentials.create("test-key", "test-secret")
    )
    val newConfig = config.withCredentials(creds)
    newConfig.credentialProvider shouldBe creds
    // Ensure other properties are unchanged
    newConfig.region shouldBe config.region
    newConfig.asyncClientConfig shouldBe config.asyncClientConfig
  }

  test("withAsyncClientConfig") {
    val config     = BedrockConfig()
    val customizer =
      (builder: BedrockRuntimeAsyncClientBuilder) =>
        // Apply some custom configuration, e.g., endpoint override (just for testing the function call)
        builder.endpointOverride(java.net.URI.create("http://localhost:8080"))
    val newConfig = config.withAsyncClientConfig(customizer)

    // Verify the new function is stored
    newConfig.asyncClientConfig shouldBeTheSameInstanceAs customizer

    // Apply the config function and check if it modifies the builder as expected
    val mockBuilder       = BedrockRuntimeAsyncClient.builder()
    val customizedBuilder = newConfig.asyncClientConfig(mockBuilder)
    // We can't directly check the endpointOverride value easily without building,
    // but we can verify the function was applied and returned the same builder instance.
    customizedBuilder shouldBeTheSameInstanceAs mockBuilder

    // Ensure other properties are unchanged
    newConfig.region shouldBe config.region
    newConfig.credentialProvider shouldBe config.credentialProvider
  }

end BedrockConfigTest
