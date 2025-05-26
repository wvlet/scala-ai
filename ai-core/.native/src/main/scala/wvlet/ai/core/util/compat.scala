package wvlet.ai.core.util

import java.security.{NoSuchAlgorithmException, SecureRandom}
import scala.util.Random

object compat:
  val random: Random = NativeSecureRandom()

  def sleep(millis: Int): Unit = Thread.sleep(millis)
