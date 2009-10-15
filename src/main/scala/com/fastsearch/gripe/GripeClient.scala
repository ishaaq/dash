package com.fastsearch.gripe

import scala.actors.Actor
import scala.actors.Actor._
import scala.actors.remote.RemoteActor
import scala.actors.remote.RemoteActor._
import scala.actors.OutputChannel
import scala.actors.remote.Node
import java.util.UUID

class GripeClient(server: Node, messageFactory: MessageFactory) extends Actor {
    def act() {
        val sink = select(server, GripeServer.name)
        link(sink)
        sink ! Syn(messageFactory.id)
        loop {
            receive {
              case Ack =>
                sender ! messageFactory.get
              case Response(response) =>
                println(">> " + response)
                sender ! messageFactory.get
              case ErrorResponse(response) =>
                println("ERR! " + response)
                sender ! messageFactory.get
              case Bye(id) =>
                exit
            }
        }
    }
}
