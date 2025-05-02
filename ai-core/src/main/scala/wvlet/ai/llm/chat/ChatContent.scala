package wvlet.ai.llm.chat

import MimeType.MARKDOWN

import java.net.URI

enum ChatContentType:
  case TEXT,
    TEXT_FILE,
    IMAGE,
    AUDIO,
    VIDEO

trait ChatContent:
  def contentType: ChatContentType

object ChatContent:
  def fromText(text: String): ChatContent = TextContent(text)

  case class TextContent(text: String) extends ChatContent:
    override def contentType = ChatContentType.TEXT

  case class TextFileContent(uri: URI, text: String, mimeType: MimeType = MimeType.TEXT)
      extends ChatContent:
    override def contentType = ChatContentType.TEXT_FILE

  case class ImageContent(
      uri: URI,
      base64data: Option[String] = None,
      mimeType: MimeType = MimeType.UNKNOWN
  ) extends ChatContent:
    override def contentType = ChatContentType.IMAGE

  case class AudioContent(
      uri: URI,
      base64data: Option[String] = None,
      mimeType: MimeType = MimeType.UNKNOWN
  ) extends ChatContent:
    override def contentType = ChatContentType.AUDIO

  case class VideoContent(
      uri: URI,
      base64data: Option[String] = None,
      mimeType: MimeType = MimeType.UNKNOWN
  ) extends ChatContent:
    override def contentType = ChatContentType.VIDEO

end ChatContent

enum MimeType:
  case UNKNOWN,
    JSON,
    MARKDOWN,
    TEXT,
    HTML,
    XHTML,
    SVG,
    PNG,
    JPEG,
    MP3,
    MP4,
    WAV,
    PDF
