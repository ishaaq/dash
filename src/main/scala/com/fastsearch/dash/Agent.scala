package com.fastsearch.dash

import java.lang.instrument.Instrumentation

object Agent {
  def agentmain(portString: String, instrumentation: Instrumentation) {
      val port = portString.toInt
      System.setProperty(Server.portProperty, portString)
      val server = new Server(port)
      server.start
  }
}
