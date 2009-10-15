package com.fastsearch.gripe

import java.lang.instrument.Instrumentation

object GripeAgent {
  def agentmain(portString: String, instrumentation: Instrumentation) {
      val port = portString.toInt
      System.setProperty(GripeServer.portProperty, portString)
      val server = new GripeServer(port)
      server.start
  }
}
