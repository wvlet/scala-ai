# AWS Bedrock Integration

Connect to AWS Bedrock for Claude model access.

## Setup

### Dependencies

```scala
libraryDependencies += "org.wvlet" %% "uni-agent-bedrock" % "2025.1.0"
```

### AWS Credentials

Ensure AWS credentials are configured:

```bash
# Environment variables
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export AWS_REGION=us-east-1

# Or use AWS CLI profile
aws configure
```

## Creating a Bedrock Chat Model

```scala
import wvlet.uni.agent.bedrock.BedrockChat

val bedrockChat = BedrockChat()
```

### With Custom Configuration

```scala
val bedrockChat = BedrockChat()
  .withRegion("us-west-2")
  .withProfile("my-profile")
```

## Using with LLMAgent

```scala
import wvlet.uni.agent.{LLMAgent, LLM}
import wvlet.uni.agent.bedrock.BedrockChat
import wvlet.uni.agent.runner.AgentRunner

// Create Bedrock chat model
val bedrockChat = BedrockChat()

// Create agent runner with Bedrock
val runner = AgentRunner(bedrockChat)

// Create agent
val agent = LLMAgent(
  name = "assistant",
  description = "Bedrock-powered assistant",
  model = LLM.Claude3Sonnet
).withSystemPrompt("You are a helpful assistant.")

// Create session and chat
val session = agent.newSession(runner)
val response = session.chat("Hello!")
println(response.text)
```

## Available Models

| Model | Identifier |
|-------|------------|
| Claude 3 Opus | `LLM.Claude3Opus` |
| Claude 3 Sonnet | `LLM.Claude3Sonnet` |
| Claude 3 Haiku | `LLM.Claude3Haiku` |

## Configuration Options

### Region

```scala
BedrockChat().withRegion("eu-west-1")
```

### AWS Profile

```scala
BedrockChat().withProfile("production")
```

### Credentials Provider

```scala
import software.amazon.awssdk.auth.credentials.*

val credentialsProvider = StaticCredentialsProvider.create(
  AwsBasicCredentials.create("key", "secret")
)

BedrockChat().withCredentialsProvider(credentialsProvider)
```

## Streaming Responses

```scala
import wvlet.uni.agent.chat.ChatObserver

val observer = new ChatObserver:
  def onText(text: String): Unit =
    print(text)

  def onComplete(): Unit =
    println()

val response = session.chat("Tell me a story", observer)
```

## Error Handling

```scala
import wvlet.uni.agent.core.AIException

try
  val response = session.chat("Hello")
catch
  case e: AIException =>
    e.statusCode match
      case 429 => println("Rate limited, retry later")
      case 401 => println("Invalid credentials")
      case _ => println(s"Error: ${e.message}")
```

## Example: Complete Application

```scala
import wvlet.uni.agent.*
import wvlet.uni.agent.chat.*
import wvlet.uni.agent.bedrock.BedrockChat
import wvlet.uni.agent.runner.AgentRunner

object BedrockExample:
  def main(args: Array[String]): Unit =
    // Initialize Bedrock
    val bedrockChat = BedrockChat()
      .withRegion("us-east-1")

    val runner = AgentRunner(bedrockChat)

    // Define agent
    val agent = LLMAgent(
      name = "coder",
      description = "Scala coding assistant",
      model = LLM.Claude3Sonnet
    )
      .withSystemPrompt("""
        You are an expert Scala 3 programmer.
        Write clean, idiomatic code with explanations.
      """)
      .withTemperature(0.3)
      .withMaxOutputTokens(2048)

    // Create session
    val session = agent.newSession(runner)

    // Chat
    val response = session.chat(
      "Write a function to calculate the nth Fibonacci number"
    )

    println(response.text)
```

## IAM Permissions

Required IAM permissions for Bedrock:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "bedrock:InvokeModel",
        "bedrock:InvokeModelWithResponseStream"
      ],
      "Resource": "arn:aws:bedrock:*:*:model/*"
    }
  ]
}
```

## Best Practices

1. **Use environment variables** for credentials
2. **Select appropriate region** for latency
3. **Handle rate limits** with retry logic
4. **Monitor costs** - Bedrock charges per token
5. **Use streaming** for better UX on long responses
