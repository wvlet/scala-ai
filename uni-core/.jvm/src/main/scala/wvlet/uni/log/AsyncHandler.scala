package wvlet.uni.log

import java.io.Flushable
import java.util
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.logging as jl

/**
  * Logging using a background thread
  */
class AsyncHandler(parent: jl.Handler) extends jl.Handler with AutoCloseable with Flushable:

  // Inline Guard functionality
  private val lock                    = new ReentrantLock()
  private def newCondition            = lock.newCondition()
  private def guard[U](body: => U): U =
    lock.lockInterruptibly()
    try body
    finally lock.unlock()

  // Inline daemon thread factory
  private def newDaemonThreadFactory(name: String): ThreadFactory =
    new ThreadFactory:
      private val group: ThreadGroup =
        new ThreadGroup(Thread.currentThread().getThreadGroup(), name)
      private val threadNumber                    = new AtomicInteger(1)
      override def newThread(r: Runnable): Thread =
        val threadName = s"${name}-${threadNumber.getAndIncrement()}"
        val thread     = new Thread(group, r, threadName)
        thread.setName(threadName)
        thread.setDaemon(true)
        thread

  private val executor = Executors.newSingleThreadExecutor(newDaemonThreadFactory("uni-log-async"))

  private val queue      = new util.ArrayDeque[jl.LogRecord]
  private val isNotEmpty = newCondition
  private val closed     = new AtomicBoolean(false)

  // Start a poller thread
  executor.submit(
    new Runnable:
      override def run(): Unit =
        while !closed.get() do
          val record: jl.LogRecord = guard {
            if queue.isEmpty then
              isNotEmpty.await()
            queue.pollFirst()
          }
          if record != null then
            parent.publish(record)
  )

  override def flush(): Unit =
    val records = Seq.newBuilder[jl.LogRecord]
    guard {
      while !queue.isEmpty do
        val record = queue.pollFirst()
        if record != null then
          records += record
    }

    records.result().map(parent.publish(_))
    parent.flush()

  override def publish(record: jl.LogRecord): Unit = guard {
    queue.addLast(record)
    isNotEmpty.signal()
  }

  override def close(): Unit =
    flush()

    if closed.compareAndSet(false, true) then
      // Wake up the poller thread
      guard {
        isNotEmpty.signalAll()
      }
      executor.shutdownNow()

  def closeAndAwaitTermination(
      timeout: Int = 10,
      timeUnit: TimeUnit = TimeUnit.MILLISECONDS
  ): Unit =
    close()
    while !executor.awaitTermination(timeout, timeUnit) do
      Thread.sleep(timeUnit.toMillis(timeout))

end AsyncHandler
