package wvlet.ai.agent.chat

trait ChatResponse(val format: ChatResponseFormat)

enum ChatResponseFormat:
  case TEXT
  case JSON
