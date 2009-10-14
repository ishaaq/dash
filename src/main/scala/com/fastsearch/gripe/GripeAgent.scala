package com.fastsearch.gripe

import java.lang.instrument.Instrumentation

import scala.actors.remote.Node

object GripeAgent {
  def agentmain(agentArgs: String, instrumentation: Instrumentation) {
      val remotePort = agentArgs.toInt
      val node = Node("127.0.0.1", remotePort)
      val client = new GripeClient(node)
      client.start
  }
}
