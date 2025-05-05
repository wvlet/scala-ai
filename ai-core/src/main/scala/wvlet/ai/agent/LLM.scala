package wvlet.ai.agent

/**
  * LLM (Large Language Model) identifier and name
  * @param id
  * @param name
  */
case class LLM(
    // Model ID used for the exact reference to the model
    id: String,
    // Name of the model product name
    name: String
):
  /**
    * Returns a new LLM instance with the given prefix added to the model ID. For the list of
    * prefixes, see:
    * https://docs.aws.amazon.com/bedrock/latest/userguide/inference-profiles-support.html
    *
    * @param prefix
    *   us, eu, apac, etc.
    */
  def withAWSCrossRegionInference(prefix: String): LLM = this.copy(id = s"${prefix}.${id}")

object LLM:
  object Bedrock:
    // Amazon Titan Models
    val TitanTextTG1Large = LLM("amazon.titan-tg1-large", "Titan Text Large")
    val TitanImageGeneratorG1V1_0 = LLM(
      "amazon.titan-image-generator-v1:0",
      "Titan Image Generator G1"
    )

    val TitanImageGeneratorG1V1 = LLM("amazon.titan-image-generator-v1", "Titan Image Generator G1")

    val TitanImageGeneratorG1V2_0 = LLM(
      "amazon.titan-image-generator-v2:0",
      "Titan Image Generator G1 v2"
    )

    val TitanTextGPremierV1_0 = LLM("amazon.titan-text-premier-v1:0", "Titan Text G1 - Premier")

    val TitanTextEmbeddingsV2 = LLM("amazon.titan-embed-g1-text-02", "Titan Text Embeddings v2")

    val TitanTextG1LiteV1_0_4k = LLM("amazon.titan-text-lite-v1:0:4k", "Titan Text G1 - Lite")

    val TitanTextG1LiteV1 = LLM("amazon.titan-text-lite-v1", "Titan Text G1 - Lite")
    val TitanTextG1ExpressV1_0_8k = LLM(
      "amazon.titan-text-express-v1:0:8k",
      "Titan Text G1 - Express"
    )

    val TitanTextG1ExpressV1 = LLM("amazon.titan-text-express-v1", "Titan Text G1 - Express")

    val TitanEmbeddingsG1TextV1_2_8k = LLM(
      "amazon.titan-embed-text-v1:2:8k",
      "Titan Embeddings G1 - Text"
    )

    val TitanEmbeddingsG1TextV1 = LLM("amazon.titan-embed-text-v1", "Titan Embeddings G1 - Text")

    val TitanTextEmbeddingsV2_0_8k = LLM(
      "amazon.titan-embed-text-v2:0:8k",
      "Titan Text Embeddings V2"
    )

    val TitanTextEmbeddingsV2_0 = LLM("amazon.titan-embed-text-v2:0", "Titan Text Embeddings V2")

    val TitanMultimodalEmbeddingsG1V1_0 = LLM(
      "amazon.titan-embed-image-v1:0",
      "Titan Multimodal Embeddings G1"
    )

    val TitanMultimodalEmbeddingsG1V1 = LLM(
      "amazon.titan-embed-image-v1",
      "Titan Multimodal Embeddings G1"
    )

    // Amazon Nova Models
    val NovaPremierV1_0_8k    = LLM("amazon.nova-premier-v1:0:8k", "Nova Premier")
    val NovaPremierV1_0_20k   = LLM("amazon.nova-premier-v1:0:20k", "Nova Premier")
    val NovaPremierV1_0_1000k = LLM("amazon.nova-premier-v1:0:1000k", "Nova Premier")
    val NovaPremierV1_0_mm    = LLM("amazon.nova-premier-v1:0:mm", "Nova Premier")
    val NovaPremierV1_0       = LLM("amazon.nova-premier-v1:0", "Nova Premier")
    val NovaProV1_0_24k       = LLM("amazon.nova-pro-v1:0:24k", "Nova Pro")
    val NovaProV1_0_300k      = LLM("amazon.nova-pro-v1:0:300k", "Nova Pro")
    val NovaProV1_0           = LLM("amazon.nova-pro-v1:0", "Nova Pro")
    val NovaLiteV1_0_24k      = LLM("amazon.nova-lite-v1:0:24k", "Nova Lite")
    val NovaLiteV1_0_300k     = LLM("amazon.nova-lite-v1:0:300k", "Nova Lite")
    val NovaLiteV1_0          = LLM("amazon.nova-lite-v1:0", "Nova Lite")
    val NovaCanvasV1_0        = LLM("amazon.nova-canvas-v1:0", "Nova Canvas")
    val NovaReelV1_0          = LLM("amazon.nova-reel-v1:0", "Nova Reel")
    val NovaReelV1_1          = LLM("amazon.nova-reel-v1:1", "Nova Reel")
    val NovaMicroV1_0_24k     = LLM("amazon.nova-micro-v1:0:24k", "Nova Micro")
    val NovaMicroV1_0_128k    = LLM("amazon.nova-micro-v1:0:128k", "Nova Micro")
    val NovaMicroV1_0         = LLM("amazon.nova-micro-v1:0", "Nova Micro")
    val NovaSonicV1_0         = LLM("amazon.nova-sonic-v1:0", "Nova Sonic")

    // Stability AI Models
    val Sdxl1_0V1_0 = LLM("stability.stable-diffusion-xl-v1:0", "SDXL 1.0")
    val Sdxl1_0V1   = LLM("stability.stable-diffusion-xl-v1", "SDXL 1.0")

    // AI21 Labs Models
    val JambaInstructV1_0 = LLM("ai21.jamba-instruct-v1:0", "Jamba-Instruct")
    val Jamba1_5LargeV1_0 = LLM("ai21.jamba-1-5-large-v1:0", "Jamba 1.5 Large")
    val Jamba1_5MiniV1_0  = LLM("ai21.jamba-1-5-mini-v1:0", "Jamba 1.5 Mini")
    // Note: Older J2 models might be missing from the provided JSON list but were in previous Bedrock versions.

    // Anthropic Models
    val ClaudeInstantV1_2_100k = LLM("anthropic.claude-instant-v1:2:100k", "Claude Instant")

    val ClaudeInstantV1 = LLM("anthropic.claude-instant-v1", "Claude Instant")
    val ClaudeV2_0_18k  = LLM("anthropic.claude-v2:0:18k", "Claude")
    val ClaudeV2_0_100k = LLM("anthropic.claude-v2:0:100k", "Claude")
    val ClaudeV2_1_18k  = LLM("anthropic.claude-v2:1:18k", "Claude")
    val ClaudeV2_1_200k = LLM("anthropic.claude-v2:1:200k", "Claude")
    val ClaudeV2_1 = LLM(
      "anthropic.claude-v2:1",
      "Claude 2.1"
    ) // Explicitly named 2.1 based on ID convention
    val ClaudeV2 = LLM("anthropic.claude-v2", "Claude")
    val Claude3Sonnet_20240229V1_0_28k = LLM(
      "anthropic.claude-3-sonnet-20240229-v1:0:28k",
      "Claude 3 Sonnet"
    )

    val Claude3Sonnet_20240229V1_0_200k = LLM(
      "anthropic.claude-3-sonnet-20240229-v1:0:200k",
      "Claude 3 Sonnet"
    )

    val Claude3Sonnet_20240229V1_0 = LLM(
      "anthropic.claude-3-sonnet-20240229-v1:0",
      "Claude 3 Sonnet"
    )

    val Claude3Haiku_20240307V1_0_48k = LLM(
      "anthropic.claude-3-haiku-20240307-v1:0:48k",
      "Claude 3 Haiku"
    )

    val Claude3Haiku_20240307V1_0_200k = LLM(
      "anthropic.claude-3-haiku-20240307-v1:0:200k",
      "Claude 3 Haiku"
    )

    val Claude3Haiku_20240307V1_0 = LLM("anthropic.claude-3-haiku-20240307-v1:0", "Claude 3 Haiku")

    val Claude3Opus_20240229V1_0_12k = LLM(
      "anthropic.claude-3-opus-20240229-v1:0:12k",
      "Claude 3 Opus"
    )

    val Claude3Opus_20240229V1_0_28k = LLM(
      "anthropic.claude-3-opus-20240229-v1:0:28k",
      "Claude 3 Opus"
    )

    val Claude3Opus_20240229V1_0_200k = LLM(
      "anthropic.claude-3-opus-20240229-v1:0:200k",
      "Claude 3 Opus"
    )

    val Claude3Opus_20240229V1_0 = LLM("anthropic.claude-3-opus-20240229-v1:0", "Claude 3 Opus")

    val Claude3_5Sonnet_20240620V1_0 = LLM(
      "anthropic.claude-3-5-sonnet-20240620-v1:0",
      "Claude 3.5 Sonnet"
    )

    val Claude3_5Sonnet_20241022V2_0 = LLM(
      "anthropic.claude-3-5-sonnet-20241022-v2:0",
      "Claude 3.5 Sonnet v2"
    )

    val Claude3_7Sonnet_20250219V1_0 = LLM(
      "anthropic.claude-3-7-sonnet-20250219-v1:0",
      "Claude 3.7 Sonnet"
    )

    val Claude3_5Haiku_20241022V1_0 = LLM(
      "anthropic.claude-3-5-haiku-20241022-v1:0",
      "Claude 3.5 Haiku"
    )

    // Cohere Models
    val CommandV14_7_4k           = LLM("cohere.command-text-v14:7:4k", "Command")
    val CommandV14                = LLM("cohere.command-text-v14", "Command")
    val CommandRV1_0              = LLM("cohere.command-r-v1:0", "Command R")
    val CommandRPlusV1_0          = LLM("cohere.command-r-plus-v1:0", "Command R+")
    val CommandLightV14_7_4k      = LLM("cohere.command-light-text-v14:7:4k", "Command Light")
    val CommandLightV14           = LLM("cohere.command-light-text-v14", "Command Light")
    val EmbedEnglishV3_0_512      = LLM("cohere.embed-english-v3:0:512", "Embed English")
    val EmbedEnglishV3            = LLM("cohere.embed-english-v3", "Embed English")
    val EmbedMultilingualV3_0_512 = LLM("cohere.embed-multilingual-v3:0:512", "Embed Multilingual")

    val EmbedMultilingualV3 = LLM("cohere.embed-multilingual-v3", "Embed Multilingual")

    // DeepSeek Models
    val DeepSeekR1V1_0 = LLM("deepseek.r1-v1:0", "DeepSeek-R1")

    // Meta Models
    val Llama3_8bInstructV1_0  = LLM("meta.llama3-8b-instruct-v1:0", "Llama 3 8B Instruct")
    val Llama3_70bInstructV1_0 = LLM("meta.llama3-70b-instruct-v1:0", "Llama 3 70B Instruct")

    val Llama3_1_8bInstructV1_0 = LLM("meta.llama3-1-8b-instruct-v1:0", "Llama 3.1 8B Instruct")

    val Llama3_1_70bInstructV1_0 = LLM("meta.llama3-1-70b-instruct-v1:0", "Llama 3.1 70B Instruct")

    val Llama3_2_11bInstructV1_0 = LLM("meta.llama3-2-11b-instruct-v1:0", "Llama 3.2 11B Instruct")

    val Llama3_2_90bInstructV1_0 = LLM("meta.llama3-2-90b-instruct-v1:0", "Llama 3.2 90B Instruct")

    val Llama3_2_1bInstructV1_0 = LLM("meta.llama3-2-1b-instruct-v1:0", "Llama 3.2 1B Instruct")

    val Llama3_2_3bInstructV1_0 = LLM("meta.llama3-2-3b-instruct-v1:0", "Llama 3.2 3B Instruct")

    val Llama3_3_70bInstructV1_0 = LLM("meta.llama3-3-70b-instruct-v1:0", "Llama 3.3 70B Instruct")

    val Llama4Scout17bInstructV1_0_128k = LLM(
      "meta.llama4-scout-17b-instruct-v1:0:128k",
      "Llama 4 Scout 17B Instruct"
    )

    val Llama4Scout17bInstructV1_0_10m = LLM(
      "meta.llama4-scout-17b-instruct-v1:0:10m",
      "Llama 4 Scout 17B Instruct"
    )

    val Llama4Scout17bInstructV1_0 = LLM(
      "meta.llama4-scout-17b-instruct-v1:0",
      "Llama 4 Scout 17B Instruct"
    )

    val Llama4Maverick17bInstructV1_0_128k = LLM(
      "meta.llama4-maverick-17b-instruct-v1:0:128k",
      "Llama 4 Maverick 17B Instruct"
    )

    val Llama4Maverick17bInstructV1_0_1m = LLM(
      "meta.llama4-maverick-17b-instruct-v1:0:1m",
      "Llama 4 Maverick 17B Instruct"
    )

    val Llama4Maverick17bInstructV1_0 = LLM(
      "meta.llama4-maverick-17b-instruct-v1:0",
      "Llama 4 Maverick 17B Instruct"
    )
    // Note: Older Llama2 models might be missing from the provided JSON list but were in previous Bedrock versions.

    // Mistral Models
    val Mistral7bInstructV0_2 = LLM("mistral.mistral-7b-instruct-v0:2", "Mistral 7B Instruct")

    val Mixtral8x7bInstructV0_1 = LLM("mistral.mixtral-8x7b-instruct-v0:1", "Mixtral 8x7B Instruct")

    val MistralLarge2402V1_0 = LLM("mistral.mistral-large-2402-v1:0", "Mistral Large (24.02)")

    val MistralSmall2402V1_0 = LLM("mistral.mistral-small-2402-v1:0", "Mistral Small (24.02)")

    val PixtralLarge2502V1_0 = LLM("mistral.pixtral-large-2502-v1:0", "Pixtral Large (25.02)")

  end Bedrock

end LLM
