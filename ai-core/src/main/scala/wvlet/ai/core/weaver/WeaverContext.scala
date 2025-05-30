package wvlet.ai.core.weaver

object WeaverContext:
  private enum DataType:
    case NIL
    case ANY
    case BOOLEAN
    case STRING
    case INTEGER
    case FLOAT
    case DOUBLE

class WeaverContext(val config: WeaverConfig = WeaverConfig()):
  import WeaverContext.DataType
  import WeaverContext.DataType.*

  private var dataType: DataType     = NIL
  private var value: Option[Any]     = None
  private var err: Option[Throwable] = None

  private var l: Long    = 0L
  private var b: Boolean = false
  private var f: Float   = 0.0f
  private var d: Double  = 0.0
  private var s: String  = ""

  def isNull: Boolean             = value.isEmpty
  def hasError: Boolean           = err.isDefined
  def getError: Option[Throwable] = err

  def reset: Unit = setNull

  def setNull: Unit =
    dataType = NIL
    value = None

  def setBoolean(v: Boolean): Unit =
    dataType = BOOLEAN
    value = Some(v)
    b = v

  def setByte(v: Byte): Unit =
    dataType = INTEGER
    value = Some(v)
    l = v

  def setChar(v: Char): Unit =
    setValue(INTEGER, v)
    l = v

  def setShort(v: Short): Unit =
    setValue(INTEGER, v)
    l = v

  def setInt(v: Int): Unit =
    setValue(INTEGER, v)
    l = v

  def setLong(v: Long): Unit =
    setValue(INTEGER, v)
    l = v

  def setFloat(v: Float): Unit =
    setValue(FLOAT, v)
    d = v

  def setDouble(v: Double): Unit =
    setValue(FLOAT, v)
    d = v

  def setString(v: String): Unit =
    setValue(STRING, v)
    s = v

  def setObject(v: Any): Unit =
    if v == null then
      setNull
    else
      setValue(ANY, v)

  private def setValue(dataType: DataType, v: Any): Unit =
    this.dataType = dataType
    if v != null then
      value = Some(v)
    else
      value = None

  def getByte: Byte =
    dataType match
      case INTEGER if l.isValidByte =>
        l.toByte
      case _ =>
        0

  def getInt: Int =
    dataType match
      case INTEGER if l.isValidInt =>
        l.toInt
      case _ =>
        0

  def getShort: Short =
    dataType match
      case INTEGER if l.isValidShort =>
        l.toShort
      case _ =>
        0

  def getChar: Char =
    dataType match
      case INTEGER if l.isValidChar =>
        l.toChar
      case _ =>
        0

  def getLong: Long =
    dataType match
      case INTEGER =>
        l
      case _ =>
        0

  def getBoolean: Boolean =
    dataType match
      case BOOLEAN =>
        b
      case _ =>
        false

  def getDouble: Double =
    dataType match
      case FLOAT =>
        d
      case _ =>
        0.0

  def getFloat: Float =
    dataType match
      case FLOAT =>
        d.toFloat
      case _ =>
        0.0f

  def getString: String =
    dataType match
      case STRING =>
        s
      case _ =>
        value.map(_.toString).getOrElse("")

  def getLastValue: Any = value.getOrElse(null)

  def setError[A](e: Throwable): Unit =
    setNull
    err = Option(e)

end WeaverContext
