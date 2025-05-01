# wvlet/scala-ai

Build production-ready LLM applications in Scala 3, without the boilerplate.

**The Challenge:** Integrating Large Language Models (LLMs) like Anthropic's Claude on AWS Bedrock or Google's Gemini on Vertex AI into applications often involves grappling with low-level Java SDKs (`aws-java-sdk-bedrockruntime`, `google-cloud-vertexai`). This typically means:

* Writing verbose code for authentication, request building, and response parsing.
* Manually crafting and parsing complex JSON payloads specific to each model.
* Handling intricate streaming APIs and callback mechanisms.
* Stitching together disparate components for tasks like RAG (Retrieval-Augmented Generation) or agentic workflows.

**The Solution: `wvlet/scala-ai`**

`wvlet/scala-ai` provides high-level, idiomatic Scala 3 abstractions that shield you from the complexities of underlying SDKs. We empower you to:

* **Focus on Application Logic:** Stop wrestling with SDK boilerplate and concentrate on building unique AI-driven features.
* **Build Faster:** Leverage pre-built components for common patterns like chat, RAG, function calling, MCP, and agent orchestrations.
* **Increase Reliability:** Benefit from Scala 3's type safety for model configurations and functional patterns for clearer, more maintainable code.
* **Seamlessly Integrate:** Easily connect LLMs with your data sources, vector databases (like DuckDB VSS), and external tools/APIs.

**(Note: This library is currently under active development. APIs may change.)**

## What You Can Build with `wvlet/scala-ai`

* **Intelligent Chatbots:** Create sophisticated conversational experiences with built-in chat memory management and effortless real-time streaming.
* **Powerful Q&A Systems:** Implement Retrieval-Augmented Generation (RAG) to answer questions based on your private documents and knowledge bases.
* **AI-Driven Agents:** Automate complex tasks by building agents that can reason, plan, and interact with external tools and APIs (Function Calling).
* **Content Generation & Summarization:** Build workflows for generating reports, summarizing text, translating languages, and more.
* **Data Analysis & Insights:** Leverage LLMs to analyze data, extract insights, and generate reports directly within your Scala applications.

## Key Features & Benefits

* **Unified LLM Interfaces (`ChatLanguageModel`, `EmbeddingModel`):** Write code once and interact with AWS Bedrock or Google Vertex AI models seamlessly. `scala-ai` handles the model-specific request/response formatting for common tasks.
* **Simplified SDK Interaction:** Forget manual JSON handling and verbose client setup. We manage authentication (using default credential providers), request building, response parsing, and basic error handling for AWS and GCP SDKs.
* **Type-Safe Configuration:** Configure LLM parameters like temperature, max tokens, and safety settings using clear Scala case classes (`BedrockChatSettings`, `VertexAiChatSettings`), catching errors at compile time, not runtime.
* **Effortless Streaming:** Integrate real-time, incremental LLM responses using Pekko Streams via the `StreamingChatLanguageModel` trait. We abstract away the complexity of managing the underlying SDK's streaming callbacks and chunk parsing.
* **Integrated RAG Components:** Easily build RAG pipelines. `scala-ai` provides composable components for document loading (`DocumentLoader`), text splitting (`TextSplitter`), embedding generation (`EmbeddingModel`), vector storage/search (`VectorStore` with DuckDB VSS support), and retrieval (`Retriever`).
* **Robust Agent Framework:** Go beyond simple LLM calls. Define agents (`AgentRunnable`) that can reason, use external tools (`Tool`), and manage their state (`AgentState`). Inspired by best practices like the Google Agent Developer Kit (ADK).
* **Systematic Evaluation:** Ensure your agents perform reliably. The `evaluation` module provides tools (`EvaluationCase`, `Evaluator`) to test agent behavior against predefined datasets and metrics.
* **Scala 3 Idioms:** Built from the ground up for Scala 3, leveraging its powerful type system and functional programming features for robust, maintainable code.

## Who Is This For?

Scala developers building AI/LLM-powered applications on the JVM who want:

* A more productive and idiomatic alternative to using raw Java SDKs (AWS, GCP).
* A type-safe environment for configuring and interacting with LLMs.
* Ready-to-use components for common LLM patterns like RAG and Agents.
* A functional approach to building complex AI workflows.

