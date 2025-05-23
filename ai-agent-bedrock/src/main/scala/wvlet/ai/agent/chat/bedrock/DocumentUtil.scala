package wvlet.ai.agent.chat.bedrock

import software.amazon.awssdk.core.SdkNumber
import software.amazon.awssdk.core.document.Document
import wvlet.airframe.codec.MessageCodec

import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*

/**
  * Utility methods for converting between JSON, Map, and AWS-SDK's Document types.
  */
object DocumentUtil:
  def fromJson(json: String): Document =
    val jsonMap = MessageCodec.of[Map[String, Any]].fromJson(json)
    fromMap(jsonMap)

  def fromArray[A](arr: Seq[A]): Document =
    val builder = Document.listBuilder()
    arr.foreach {
      case null =>
        builder.addNull()
      case s: String =>
        builder.addString(s)
      case b: Boolean =>
        builder.addBoolean(b)
      case i: Int =>
        builder.addNumber(i)
      case l: Long =>
        builder.addNumber(l)
      case f: Float =>
        builder.addNumber(f)
      case d: Double =>
        builder.addNumber(d)
      case b: BigDecimal =>
        builder.addNumber(b.underlying)
      case b: BigInt =>
        builder.addNumber(b.underlying)
      case b: java.math.BigDecimal =>
        builder.addNumber(b)
      case b: java.math.BigInteger =>
        builder.addNumber(b)
      case d: Document =>
        builder.addDocument(d)
      case m: Map[?, ?] =>
        builder.addDocument(fromMap(m.asInstanceOf[Map[String, Any]]))
      case lst: Seq[?] =>
        builder.addDocument(fromArray(lst))
    }

    builder.build()

  end fromArray

  def fromMap(m: Map[String, Any]): Document =
    val builder = Document.mapBuilder()

    @tailrec
    def putAny(key: String, v: Any): Unit =
      v match
        case null =>
          builder.putNull(key)
        case o: Option[?] =>
          o match
            case Some(v) =>
              putAny(key, v)
            case None =>
            // do not put optional values
        case d: Document =>
          builder.putDocument(key, d)
        case m: Map[?, ?] =>
          builder.putDocument(key, fromMap(m.asInstanceOf[Map[String, Any]]))
        case lst: Seq[?] =>
          builder.putDocument(key, fromArray(lst))
        case v: Int =>
          builder.putNumber(key, v)
        case l: Long =>
          builder.putNumber(key, l)
        case d: Double =>
          builder.putNumber(key, d)
        case f: Float =>
          builder.putNumber(key, f)
        case b: Boolean =>
          builder.putBoolean(key, b)
        case s: String =>
          builder.putString(key, s)
        case v =>
          builder.putString(key, v.toString)

    m.foreach { case (k, v) =>
      putAny(k, v)
    }
    builder.build()

  end fromMap

  def toMap(d: Document): Map[String, Any] =
    def toAny(d: Document): Any =
      d match
        case d if d.isNull =>
          null
        case d if d.isString =>
          d.asString()
        case d if d.isBoolean =>
          d.asBoolean()
        case d if d.isNumber =>
          d.asNumber()
        case d if d.isList =>
          d.asList().asScala.map(toAny).toList
        case d if d.isMap =>
          d.asMap()
            .asScala
            .map { case (k, v) =>
              k -> toAny(v)
            }
            .toMap
        case v =>
          v

    val m = d.asMap().asScala
    m.map { case (k, v) =>
        k -> toAny(v)
      }
      .toMap

end DocumentUtil
