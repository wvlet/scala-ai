package wvlet.ai.util.log

case class LogSource(fileName: String, line: Int, col: Int):
  def fileLocation = s"${fileName}:${line}"
