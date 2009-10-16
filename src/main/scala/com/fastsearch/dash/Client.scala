package com.fastsearch.dash

import scala.actors.Actor
import scala.actors.Actor._
import scala.actors.remote.RemoteActor
import scala.actors.remote.RemoteActor._
import scala.actors.OutputChannel
import scala.actors.remote.Node
import java.util.UUID

class Client(server: Node, messageFactory: MessageFactory) extends Actor {
    def act() {
        val sink = select(server, Server.name)
        link(sink)
        sink ! Syn(messageFactory.id)
        loop {
            receive {
              case Ack =>
                sender ! messageFactory.get
              case Print(string) =>
                print(string)
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
