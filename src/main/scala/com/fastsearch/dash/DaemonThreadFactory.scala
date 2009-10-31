package com.fastsearch.dash

import java.util.concurrent.ThreadFactory

object DaemonThreadFactory extends ThreadFactory {
    def newThread(r: Runnable): Thread = {
      val thread = new Thread(r)
      thread.setDaemon(true)
      thread
    }
}
