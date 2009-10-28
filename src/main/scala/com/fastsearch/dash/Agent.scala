package com.fastsearch.dash

import java.lang.instrument.Instrumentation
import Constants._

object Agent {
  def agentmain(args: String, instrumentation: Instrumentation) {
      val arguments = args.split(",")
      val port = arguments(0).toInt
      System.setProperty(dashHomeProperty, arguments(1))
      val server = new Server(port)
      server.start
  }
}
