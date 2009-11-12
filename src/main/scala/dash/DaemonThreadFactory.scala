package dash

import java.util.concurrent.ThreadFactory

/**
 * A thread-factory that only doles out daemon threads. Required in order to ensure
 * we only construct daemon threads on the Server - if not we run the risk of affecting
 * the Server app's shutdown mechanism.
 */
object DaemonThreadFactory extends ThreadFactory {
    def newThread(r: Runnable): Thread = {
      val thread = new Thread(r)
      thread.setDaemon(true)
      thread
    }
}
