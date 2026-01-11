package wvlet.uni.core.util

import scala.util.Random

/**
  * Create a secure random generator, that prevents next-value predication-based attack
  */
object SecureRandom:
  def getInstance: Random = compat.random
