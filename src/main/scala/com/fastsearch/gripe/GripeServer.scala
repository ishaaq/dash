package com.fastsearch.gripe

import scala.actors.Actor
import scala.actors.Actor._
import scala.actors.OutputChannel
import scala.actors.remote.RemoteActor
import scala.actors.remote.RemoteActor._

class GripeServer(port: Int, messageFactory: MessageFactory) extends Actor {
    def act() {
        alive(port)
        register(GripeServer.name, self)

        loop {
          receive {
            case Ack => sender ! messageFactory.get
            case Response(response) =>
              println("server got response: " + response)
              sender ! messageFactory.get
            case HaltAck => exit
          }
        }
    }
}

object GripeServer {
  val name = 'gripe
}
