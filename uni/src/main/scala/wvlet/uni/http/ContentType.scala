/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.uni.http

/**
  * MIME type representation for Content-Type headers.
  *
  * ContentType is represented as a simple string wrapper for common cases, with parsing support for extracting
  * parameters like charset.
  */
opaque type ContentType = String

object ContentType:
  // Constructor
  def apply(value: String): ContentType = value

  // Application types
  val ApplicationJson: ContentType           = "application/json"
  val ApplicationXml: ContentType            = "application/xml"
  val ApplicationFormUrlEncoded: ContentType = "application/x-www-form-urlencoded"
  val ApplicationOctetStream: ContentType    = "application/octet-stream"
  val ApplicationMsgPack: ContentType        = "application/msgpack"
  val ApplicationPdf: ContentType            = "application/pdf"
  val ApplicationZip: ContentType            = "application/zip"
  val ApplicationGzip: ContentType           = "application/gzip"
  val ApplicationJavaScript: ContentType     = "application/javascript"

  // Text types
  val TextPlain: ContentType       = "text/plain"
  val TextHtml: ContentType        = "text/html"
  val TextCss: ContentType         = "text/css"
  val TextJavaScript: ContentType  = "text/javascript"
  val TextCsv: ContentType         = "text/csv"
  val TextXml: ContentType         = "text/xml"
  val TextMarkdown: ContentType    = "text/markdown"
  val TextEventStream: ContentType = "text/event-stream"

  // Multipart types
  val MultipartFormData: ContentType    = "multipart/form-data"
  val MultipartMixed: ContentType       = "multipart/mixed"
  val MultipartAlternative: ContentType = "multipart/alternative"

  // Image types
  val ImagePng: ContentType  = "image/png"
  val ImageJpeg: ContentType = "image/jpeg"
  val ImageGif: ContentType  = "image/gif"
  val ImageWebp: ContentType = "image/webp"
  val ImageSvg: ContentType  = "image/svg+xml"
  val ImageIcon: ContentType = "image/x-icon"

  // Audio types
  val AudioMpeg: ContentType = "audio/mpeg"
  val AudioOgg: ContentType  = "audio/ogg"
  val AudioWav: ContentType  = "audio/wav"
  val AudioWebm: ContentType = "audio/webm"

  // Video types
  val VideoMp4: ContentType  = "video/mp4"
  val VideoWebm: ContentType = "video/webm"
  val VideoOgg: ContentType  = "video/ogg"

  // Font types
  val FontWoff: ContentType  = "font/woff"
  val FontWoff2: ContentType = "font/woff2"
  val FontTtf: ContentType   = "font/ttf"
  val FontOtf: ContentType   = "font/otf"

  def parse(value: String): Option[ContentType] =
    if value.isEmpty then None
    else Some(value)

  def unapply(value: String): Option[ContentType] = parse(value)

  extension (ct: ContentType)
    def value: String = ct

    def mediaType: String =
      val idx = ct.indexOf('/')
      if idx >= 0 then ct.substring(0, idx).toLowerCase else ct.toLowerCase

    def subType: String =
      val slashIdx     = ct.indexOf('/')
      val semicolonIdx = ct.indexOf(';')
      if slashIdx < 0 then ""
      else if semicolonIdx < 0 then ct.substring(slashIdx + 1).trim.toLowerCase
      else ct.substring(slashIdx + 1, semicolonIdx).trim.toLowerCase

    def fullType: String =
      val semicolonIdx = ct.indexOf(';')
      val base         = if semicolonIdx < 0 then ct else ct.substring(0, semicolonIdx)
      base.trim.toLowerCase

    def charset: Option[String] =
      val lower = ct.toLowerCase
      val idx   = lower.indexOf("charset=")
      if idx < 0 then None
      else
        val start = idx + 8
        val end   = ct.indexOf(';', start) match
          case -1 => ct.length
          case n  => n
        Some(ct.substring(start, end).trim.stripPrefix("\"").stripSuffix("\""))

    def withCharset(cs: String): ContentType =
      s"${fullType}; charset=${cs}"

    def withUtf8Charset: ContentType =
      withCharset("UTF-8")

    def withBoundary(boundary: String): ContentType =
      s"${fullType}; boundary=${boundary}"

    def isText: Boolean        = mediaType == "text"
    def isApplication: Boolean = mediaType == "application"
    def isMultipart: Boolean   = mediaType == "multipart"
    def isImage: Boolean       = mediaType == "image"
    def isAudio: Boolean       = mediaType == "audio"
    def isVideo: Boolean       = mediaType == "video"
    def isFont: Boolean        = mediaType == "font"

    def isJson: Boolean = fullType == "application/json" ||
      (mediaType == "application" && subType.endsWith("+json"))

    def isXml: Boolean = fullType == "application/xml" ||
      fullType == "text/xml" ||
      (mediaType == "application" && subType.endsWith("+xml"))
