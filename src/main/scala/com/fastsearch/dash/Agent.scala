package com.fastsearch.dash

import java.lang.instrument.Instrumentation
import Constants._

object Agent {
  def agentmain(args: String, instrumentation: Instrumentation) {
      val arguments = args.split(",")
      val port = arguments(0).toInt
      val dashHome = arguments(1)
      val id = Symbol(arguments(2))
      new Server(id, port, arguments(1)).start
  }
}
