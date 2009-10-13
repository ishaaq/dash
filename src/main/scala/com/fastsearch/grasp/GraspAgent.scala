package com.fastsearch.grasp

import java.lang.instrument.Instrumentation

object GraspAgent {
  def agentmain(agentArgs: String, instrumentation: Instrumentation) {
      println("hello world!")
  }
}
