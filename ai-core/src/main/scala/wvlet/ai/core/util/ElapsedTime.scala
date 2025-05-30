package wvlet.ai.core.util

import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

import wvlet.ai.core.surface.{Surface, Zero}

import scala.annotation.tailrec
import scala.concurrent.duration.*

/**
  * Scala version of io.airlift.unit.Duration
  */
case class ElapsedTime(value: Double, unit: TimeUnit) extends Comparable[ElapsedTime]:
  import ElapsedTime.*

  require(!value.isInfinite, s"infinite size")
  require(!value.isNaN, s"value is not a number")
  require(value >= 0, s"negative size ${value}, ${unit}")

  def toMillis: Double = roundTo(MILLISECONDS)

  def valueIn(targetUnit: TimeUnit): Double =
    value * millisPerTimeUnit(unit) * 1.0 / millisPerTimeUnit(targetUnit)

  def roundTo(targetUnit: TimeUnit): Double        = Math.floor(valueIn(targetUnit) + 0.5d)
  def convertTo(targetUnit: TimeUnit): ElapsedTime = ElapsedTime(valueIn(targetUnit), targetUnit)

  def convertToMostSuccinctTimeUnit: ElapsedTime =
    @tailrec
    def unitToUse(current: TimeUnit, unitsToTest: List[TimeUnit]): TimeUnit =
      if unitsToTest.isEmpty then
        current
      else
        val next = unitsToTest.head
        if valueIn(next) > 0.9999 then
          unitToUse(next, unitsToTest.tail)
        else
          current

    convertTo(unitToUse(NANOSECONDS, units))

  override def toString: String =
    val magnitude            = valueIn(unit)
    val timeUnitAbbreviation = timeUnitToString(unit)
    return f"${magnitude}%.2f${timeUnitAbbreviation}"

  override def compareTo(o: ElapsedTime) = valueIn(MILLISECONDS).compareTo(o.valueIn(MILLISECONDS))

end ElapsedTime

object ElapsedTime:
  Zero.register(Surface.of[ElapsedTime], ElapsedTime.succinctMillis(0))
  Zero.register(Surface.of[TimeUnit], TimeUnit.NANOSECONDS)

  def units = List(NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS)

  private val strReprPattern = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)?)\\s*([a-zA-Z]+)\\s*$")

  def nanosSince(start: Long): ElapsedTime    = succinctNanos(System.nanoTime - start)
  def succinctNanos(nanos: Long): ElapsedTime = succinctDuration(nanos.toDouble, NANOSECONDS)
  def succinctDuration(value: Double, unit: TimeUnit): ElapsedTime =
    ElapsedTime(value, unit).convertToMostSuccinctTimeUnit

  def succinctMillis(milliSeconds: Long): ElapsedTime =
    ElapsedTime(milliSeconds.toDouble, MILLISECONDS).convertToMostSuccinctTimeUnit

  def apply(elapsedTimeStr: String): ElapsedTime = parse(elapsedTimeStr)

  def parse(s: String): ElapsedTime =
    val m = strReprPattern.matcher(s)
    if !m.matches() then
      throw new IllegalArgumentException(
        s"${s} is not a valid duration string. Expected format: '<number><unit>', e.g., 5ms, 2.5h, 1d"
      )
    val value      = m.group(1).toDouble
    val unitString = m.group(2)
    ElapsedTime(value, valueOfTimeUnit(unitString))

  def millisPerTimeUnit(timeUnit: TimeUnit): Double =
    timeUnit match
      case NANOSECONDS =>
        1.0 / 1000000.0
      case MICROSECONDS =>
        1.0 / 1000.0
      case MILLISECONDS =>
        1
      case SECONDS =>
        1000
      case MINUTES =>
        1000 * 60
      case HOURS =>
        1000 * 60 * 60
      case DAYS =>
        1000 * 60 * 60 * 24
      case null =>
        throw new IllegalArgumentException("Unsupported time unit " + timeUnit)

  def timeUnitToString(timeUnit: TimeUnit): String =
    timeUnit match
      case NANOSECONDS =>
        "ns"
      case MICROSECONDS =>
        "us"
      case MILLISECONDS =>
        "ms"
      case SECONDS =>
        "s"
      case MINUTES =>
        "m"
      case HOURS =>
        "h"
      case DAYS =>
        "d"
      case null =>
        throw new IllegalArgumentException("Unsupported time unit " + timeUnit)

  def valueOfTimeUnit(timeUnitString: String): TimeUnit =
    timeUnitString match
      case "ns" =>
        NANOSECONDS
      case "us" =>
        MICROSECONDS
      case "ms" =>
        MILLISECONDS
      case "s" =>
        SECONDS
      case "m" =>
        MINUTES
      case "h" =>
        HOURS
      case "d" =>
        DAYS
      case _ =>
        throw new IllegalArgumentException("Unknown time unit: " + timeUnitString)

end ElapsedTime
