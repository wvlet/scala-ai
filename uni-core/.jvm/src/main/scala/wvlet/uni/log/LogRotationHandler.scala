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
package wvlet.uni.log

import wvlet.uni.log.LogFormatter
import LogFormatter.AppLogFormatter

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.Flushable
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.logging as jl
import java.util.logging.ErrorManager
import java.util.zip.GZIPOutputStream
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.Using

/**
  * Writing logs to a file without rotation. This is just a wrapper of LogRotationHandler
  * @param fileName
  * @param formatter
  * @param logFileExt
  */
class FileHandler(
    fileName: String,
    formatter: LogFormatter = AppLogFormatter,
    logFileExt: String = ".log"
) extends LogRotationHandler(
      fileName,
      maxNumberOfFiles = Integer.MAX_VALUE,
      maxSizeInBytes = Long.MaxValue,
      formatter = formatter,
      logFileExt = logFileExt
    )

/**
  * Log rotation handler with time-based (daily) and size-based rotation. Rotated files are
  * compressed with gzip.
  */
class LogRotationHandler(
    fileName: String,
    maxNumberOfFiles: Int = 100,      // Rotate up to 100 files
    maxSizeInBytes: Long = 104857600, // 100 MB
    formatter: LogFormatter = AppLogFormatter,
    logFileExt: String = ".log",
    tempFileExt: String = ".tmp"
) extends jl.Handler
    with AutoCloseable
    with Flushable:

  recoverTempFiles(fileName)
  setFormatter(formatter)

  private val fileNameStem =
    if fileName.endsWith(logFileExt) then
      fileName.substring(0, fileName.length - logFileExt.length)
    else
      fileName

  private val logFile       = new File(fileName)
  private val logDir        = Option(logFile.getParentFile).getOrElse(new File("."))
  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  @volatile
  private var writer: Writer = null

  @volatile
  private var currentFileSize = 0L

  @volatile
  private var currentDate: LocalDate = null

  // Lock for thread-safe file operations
  private val lock = new Object

  private def initWriter(): Unit = lock.synchronized:
    if writer == null then
      logDir.mkdirs()
      val append = logFile.exists()
      if append then
        currentFileSize = logFile.length()
        // Use file's last modified date to ensure proper rotation after restart
        currentDate =
          Instant.ofEpochMilli(logFile.lastModified()).atZone(ZoneId.systemDefault()).toLocalDate
      else
        currentFileSize = 0L
        currentDate = LocalDate.now()
      writer =
        new OutputStreamWriter(
          new BufferedOutputStream(new FileOutputStream(logFile, append)),
          StandardCharsets.UTF_8
        )

  override def flush(): Unit = lock.synchronized:
    if writer != null then
      Try(writer.flush())

  private def toException(t: Throwable) = new Exception(t.getMessage, t)

  override def publish(record: jl.LogRecord): Unit =
    if isLoggable(record) then
      Try(formatter.format(record)) match
        case Success(message) =>
          Try(writeMessage(s"${message}\n")) match
            case Success(_) =>
            // do nothing
            case Failure(e) =>
              reportError(null, toException(e), ErrorManager.WRITE_FAILURE)
        case Failure(e) =>
          reportError(null, toException(e), ErrorManager.FORMAT_FAILURE)

  private def writeMessage(message: String): Unit = lock.synchronized:
    checkRotation()
    if writer == null then
      initWriter()
    writer.write(message)
    writer.flush()
    currentFileSize += message.getBytes(StandardCharsets.UTF_8).length

  private def checkRotation(): Unit =
    val today         = LocalDate.now()
    val needsRotation =
      writer != null && (
        (currentDate != null && !currentDate.equals(today)) ||
          (maxSizeInBytes != Long.MaxValue && currentFileSize >= maxSizeInBytes)
      )

    if needsRotation then
      rotate()
      currentDate = today
      currentFileSize = 0L

  private def rotate(): Unit =
    // Close current writer
    if writer != null then
      Try(writer.close()) match
        case Failure(e) =>
          reportError(null, toException(e), ErrorManager.CLOSE_FAILURE)
        case Success(_) =>
        // Closing succeeded
      writer = null

    if logFile.exists() && logFile.length() > 0 then
      // Find the next available index for today's date
      val dateStr =
        if currentDate != null then
          currentDate.format(dateFormatter)
        else
          LocalDate.now().format(dateFormatter)
      val index           = findNextIndex(dateStr)
      val rotatedFileName = s"${fileNameStem}-${dateStr}.${index}${logFileExt}"
      val rotatedFile     = new File(rotatedFileName)

      // Rename current log file
      if logFile.renameTo(rotatedFile) then
        // Compress the rotated file
        compressFile(rotatedFile)
      else
        reportError(
          s"Failed to rename ${logFile} to ${rotatedFile}",
          null,
          ErrorManager.GENERIC_FAILURE
        )

      // Clean up old files
      cleanupOldFiles()

  end rotate

  private def findNextIndex(dateStr: String): Int =
    val pattern =
      s"${java.util.regex.Pattern.quote(new File(fileNameStem).getName)}-${dateStr}\\.(\\d+)${java
          .util
          .regex
          .Pattern
          .quote(logFileExt)}\\.gz".r
    val existingIndices = Option(logDir.listFiles())
      .getOrElse(Array.empty[File])
      .flatMap { f =>
        f.getName match
          case pattern(idx) =>
            Some(idx.toInt)
          case _ =>
            None
      }

    if existingIndices.isEmpty then
      0
    else
      existingIndices.max + 1

  private def compressFile(file: File): Unit =
    val gzFile = new File(s"${file.getAbsolutePath}.gz")
    Try {
      Using.resources(
        new FileInputStream(file),
        new GZIPOutputStream(new FileOutputStream(gzFile))
      ) { (in, out) =>
        val buffer = new Array[Byte](8192)
        var len    = in.read(buffer)
        while len > 0 do
          out.write(buffer, 0, len)
          len = in.read(buffer)
      }
      file.delete()
    } match
      case Failure(e) =>
        reportError(s"Failed to compress ${file}", toException(e), ErrorManager.GENERIC_FAILURE)
      case Success(_) =>
      // Compression successful

  private def cleanupOldFiles(): Unit =
    if maxNumberOfFiles < Integer.MAX_VALUE then
      val stemName     = new File(fileNameStem).getName
      val rotatedFiles = Option(logDir.listFiles())
        .getOrElse(Array.empty[File])
        .filter { f =>
          val name = f.getName
          name.startsWith(s"${stemName}-") && name.endsWith(s"${logFileExt}.gz")
        }
        .sortBy(_.lastModified())

      if rotatedFiles.length > maxNumberOfFiles then
        val filesToDelete = rotatedFiles.take(rotatedFiles.length - maxNumberOfFiles)
        filesToDelete.foreach { f =>
          if !f.delete() then
            reportError(s"Failed to delete old log file ${f}", null, ErrorManager.GENERIC_FAILURE)
        }

  override def close(): Unit = lock.synchronized:
    if writer != null then
      Try(writer.close()) match
        case Success(_) =>
        // do nothing
        case Failure(e) =>
          reportError(null, toException(e), ErrorManager.CLOSE_FAILURE)
      writer = null

  private def recoverTempFiles(logPath: String): Unit =
    // Recover orphaned temp files
    for
      logPathFile <- Option(new File(logPath).getParentFile)
      fileList    <- Option(logPathFile.listFiles)
      tempFile    <- fileList.filter(_.getName.endsWith(tempFileExt))
    do
      val newName = tempFile.getName().substring(0, tempFile.getName().length() - tempFileExt.size)
      val newFile = new File(tempFile.getParent, newName + logFileExt)

      if !tempFile.renameTo(newFile) then
        reportError(
          s"Failed to rename temp file ${tempFile} to ${newFile}",
          null,
          ErrorManager.OPEN_FAILURE
        )

end LogRotationHandler
