package wvlet.ai.agent.chat

trait ChatResponse:
  def format: ChatResponseFormat

enum ChatResponseFormat:
  case TEXT
  case JSON
