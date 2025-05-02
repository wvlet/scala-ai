package wvlet.ai.agent

import wvlet.ai.agent.chat.ToolSpec

case class LLMAgent(
    name: String,
    description: String,
    model: String,
    /**
      * Instruction for the model to follow.
      */
    instruction: String = "",
    tools: List[ToolSpec] = Nil,
    modelConfig: ModelConfig = ModelConfig()
)

case class ModelConfig(
    /**
      * Controls the randomness of token selection. Lower values produce less random responses,
      * while higher values result in more diverse or creative outputs.
      */
    temperature: Option[Double] = None,

    /**
      * Selects tokens based on the cumulative probability of the most likely tokens. Lower values
      * lead to less random responses, higher values increase randomness.
      */
    topP: Option[Double] = None,

    /**
      * Samples from the top K most probable tokens at each step. Lower values result in less random
      * responses, higher values increase randomness. Applied before topP filtering.
      */
    topK: Option[Int] = None,

    /**
      * The maximum number of tokens allowed in the generated response.
      */
    maxOutputTokens: Option[Long] = None,

    /**
      * A list of strings that, if encountered one of them, will stop the generation process.
      */
    stopSequences: List[String] = Nil,

    /**
      * The number of response candidates to generate. Useful for sampling multiple outputs.
      */
    candidateCount: Option[Int] = None
)
