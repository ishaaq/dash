package dash

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * A thread-factory that only doles out daemon threads. Required in order to ensure
 * we only construct daemon threads on the Server - if not we run the risk of affecting
 * the Server app's shutdown mechanism.
 */
object DaemonThreadFactory extends ThreadFactory {
    private val threadNumber = new AtomicInteger(1);
    def newThread(r: Runnable): Thread = {
      val thread = new Thread(r, "daemon-thread-" + threadNumber.incrementAndGet())
      thread.setDaemon(true)
      thread
    }
}
