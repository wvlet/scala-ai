package wvlet.ai.core.log

import wvlet.ai.core.log.{LogLevel, Logger}
import wvlet.airspec.AirSpec

import java.util.logging
import java.util.logging.{Handler, Level}

class LoggerTest extends AirSpec:
  private val myHandler =
    new Handler():
      override def publish(record: logging.LogRecord): Unit = ???

      override def flush(): Unit = ???

      override def close(): Unit = ???

  test("should create a logger and retrieve its name") {
    val logger = Logger("testLogger")
    logger.getName shouldBe "testLogger"
  }

  test("should set and get log levels") {
    val logger = Logger("testLogger")
    logger.setLogLevel(LogLevel.DEBUG)
    logger.getLogLevel shouldBe LogLevel.DEBUG

    logger.setLogLevel(LogLevel.ERROR)
    logger.getLogLevel shouldBe LogLevel.ERROR
  }

  test("should add and clear handlers") {
    val logger = Logger("testLogger")
    logger.addHandler(myHandler)
    logger.getHandlers shouldContain myHandler

    logger.clearHandlers
    logger.getHandlers shouldBe empty
  }

  test("should log messages at different levels") {
    val logger = Logger("testLogger")
    logger.setLogLevel(LogLevel.INFO)

    logger.isEnabled(LogLevel.INFO) shouldBe true
    logger.isEnabled(LogLevel.DEBUG) shouldBe false
  }

  test("should suppress logs temporarily") {
    val logger = Logger("testLogger")
    logger.setLogLevel(LogLevel.INFO)

    logger.suppressLogs {
      logger.getLogLevel shouldBe LogLevel.OFF
    }

    logger.getLogLevel shouldBe LogLevel.INFO
  }

  test("should suppress warnings temporarily") {
    val logger = Logger("testLogger")
    logger.setLogLevel(LogLevel.INFO)

    logger.suppressWarnings {
      logger.getLogLevel shouldBe LogLevel.ERROR
    }

    logger.getLogLevel shouldBe LogLevel.INFO
  }

  test("should reset log level") {
    val logger = Logger("testLogger")
    logger.setLogLevel(LogLevel.DEBUG)
    logger.resetLogLevel
    logger.getLogLevel shouldBe LogLevel.INFO // Default log level
  }

  test("should clear all handlers including parent handlers") {
    val logger = Logger("testLogger")

    logger.addHandler(myHandler)
    logger.clearAllHandlers
    logger.getHandlers shouldBe empty
  }

end LoggerTest
