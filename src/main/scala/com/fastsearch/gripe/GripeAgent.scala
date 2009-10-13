package com.fastsearch.gripe

import java.lang.instrument.Instrumentation

object GripeAgent {
  def agentmain(agentArgs: String, instrumentation: Instrumentation) {
      println("hello world!")
  }
}
