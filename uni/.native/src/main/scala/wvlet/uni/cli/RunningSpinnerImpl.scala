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
package wvlet.uni.cli

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import scala.compiletime.uninitialized

/**
  * Scala Native-specific RunningSpinner implementation.
  *
  * Note: For simplicity, this uses a simple polling approach. A more sophisticated implementation
  * could use native threading.
  */
private[cli] object RunningSpinnerImpl:
  def create(config: Spinner): RunningSpinner =
    if config.enabled && CliEnv.isInteractive then
      NativeRunningSpinner(config)
    else
      DisabledSpinner(config)

/**
  * Disabled spinner that just logs text without animation.
  */
private class DisabledSpinner(config: Spinner) extends RunningSpinner:
  private val currentText = new AtomicReference[String](config.text)
  private val running     = new AtomicBoolean(true)

  // Print initial text if any
  if currentText.get().nonEmpty then
    config.stream.println(currentText.get())

  override def text: String                  = currentText.get()
  override def text_=(newText: String): Unit = currentText.set(newText)

  override def succeed(text: String): Unit =
    val finalText =
      if text.nonEmpty then
        text
      else
        currentText.get()
    config.stream.println(s"${Symbols.successColored} ${finalText}")
    running.set(false)

  override def fail(text: String): Unit =
    val finalText =
      if text.nonEmpty then
        text
      else
        currentText.get()
    config.stream.println(s"${Symbols.errorColored} ${finalText}")
    running.set(false)

  override def warn(text: String): Unit =
    val finalText =
      if text.nonEmpty then
        text
      else
        currentText.get()
    config.stream.println(s"${Symbols.warningColored} ${finalText}")
    running.set(false)

  override def info(text: String): Unit =
    val finalText =
      if text.nonEmpty then
        text
      else
        currentText.get()
    config.stream.println(s"${Symbols.infoColored} ${finalText}")
    running.set(false)

  override def stop(): Unit        = running.set(false)
  override def clear(): Unit       = running.set(false)
  override def isSpinning: Boolean = running.get()

end DisabledSpinner

/**
  * Native running spinner.
  *
  * Note: This implementation uses a separate thread for animation. The thread-based approach is
  * simpler than using native timers.
  */
private class NativeRunningSpinner(config: Spinner) extends RunningSpinner:
  private val currentText = new AtomicReference[String](config.text)
  private val running     = new AtomicBoolean(true)
  private val frameIndex  = new AtomicInteger(0)
  private val frames      = config.style.frames
  @volatile
  private var animationThread: Thread = uninitialized

  // Start the spinner
  init()

  private def init(): Unit =
    if config.hideCursor then
      Terminal.hideCursor(config.stream)

    animationThread = new Thread(() => runAnimation())
    animationThread.setDaemon(true)
    animationThread.start()

  private def runAnimation(): Unit =
    while running.get() do
      render()
      try
        Thread.sleep(config.style.interval.toLong)
      catch
        case _: InterruptedException =>
          ()

  private def render(): Unit =
    if running.get() then
      val idx   = frameIndex.getAndUpdate(i => (i + 1) % frames.size)
      val frame = config.color(frames(idx))
      val txt   = currentText.get()
      val line  =
        if txt.nonEmpty then
          s"${frame} ${txt}"
        else
          frame
      config.stream.print(s"\r${line}")
      config.stream.print("\u001b[K")
      config.stream.flush()

  private def stopAnimation(): Unit =
    running.set(false)
    if animationThread != null then
      animationThread.interrupt()
    if config.hideCursor then
      Terminal.showCursor(config.stream)

  private def finishWithSymbol(symbol: String, finalText: String): Unit =
    stopAnimation()
    val txt =
      if finalText.nonEmpty then
        finalText
      else
        currentText.get()
    val line =
      if txt.nonEmpty then
        s"${symbol} ${txt}"
      else
        symbol
    config.stream.print(s"\r${line}")
    config.stream.print("\u001b[K")
    config.stream.println()
    config.stream.flush()

  override def text: String                  = currentText.get()
  override def text_=(newText: String): Unit = currentText.set(newText)

  override def succeed(text: String): Unit = finishWithSymbol(Symbols.successColored, text)

  override def fail(text: String): Unit = finishWithSymbol(Symbols.errorColored, text)

  override def warn(text: String): Unit = finishWithSymbol(Symbols.warningColored, text)

  override def info(text: String): Unit = finishWithSymbol(Symbols.infoColored, text)

  override def stop(): Unit =
    stopAnimation()
    val txt = currentText.get()
    if txt.nonEmpty then
      config.stream.print(s"\r${txt}")
      config.stream.print("\u001b[K")
      config.stream.println()
      config.stream.flush()

  override def clear(): Unit =
    stopAnimation()
    config.stream.print("\r")
    config.stream.print("\u001b[K")
    config.stream.flush()

  override def isSpinning: Boolean = running.get()

end NativeRunningSpinner
