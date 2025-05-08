package wvlet.ai.log

import scala.scalajs.js

/**
  * Use scalajs.js.Date to foramt timestamps
  */
object LogTimestampFormatter:
  def formatTimestamp(timeMillis: Long): String = new js.Date(timeMillis.toDouble).toISOString()

  def formatTimestampWithoutSpace(timeMillis: Long): String = new js.Date(timeMillis.toDouble)
    .toISOString()
