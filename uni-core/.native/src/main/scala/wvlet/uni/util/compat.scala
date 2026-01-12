package wvlet.uni.util

import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import scala.util.Random

object compat:
  val random: Random = NativeSecureRandom()

  def sleep(millis: Int): Unit = Thread.sleep(millis)
