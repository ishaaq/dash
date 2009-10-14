package com.fastsearch.gripe

import scala.actors.Actor
import scala.actors.Actor._
import scala.actors.remote.RemoteActor
import scala.actors.remote.RemoteActor._
import scala.actors.OutputChannel
import scala.actors.remote.Node

class GripeClient(server: Node) extends Actor {
    def act() {
        println("client: ready")
        val sink = select(server, GripeServer.name)
        link(sink)
        sink ! Ack
        loop {
            receive {
              case Command(command) =>
                println("client: command received: " + command)
                sink ! new Response("response")
              case Halt =>
                println("client: halt")
                sink ! HaltAck
                Thread.sleep(1000)
                exit
            }
        }
    }
}
