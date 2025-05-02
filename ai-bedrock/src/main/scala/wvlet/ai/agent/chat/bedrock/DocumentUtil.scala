package wvlet.ai.agent.chat.bedrock

import software.amazon.awssdk.core.document.Document
import scala.jdk.CollectionConverters.*

object DocumentUtil:
  def fromString(s: String): Document = Document.fromString(s)

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
        case s: String =>
          builder.putString(key, s)
        case v =>
          builder.putString(key, v.toString)

    m.foreach { case (k, v) =>
      putAny(k, v)
    }
    builder.build()

  end fromMap

end DocumentUtil
