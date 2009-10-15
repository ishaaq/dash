package com.fastsearch.gripe

import java.util.UUID
import scala.actors.Actor
import scala.actors.Actor._
import scala.actors.OutputChannel
import scala.actors.remote.RemoteActor
import scala.actors.remote.RemoteActor._
import groovy.lang.GroovyShell
import scala.collection.mutable.Map

class GripeServer(port: Int) extends Actor {
    private val sessions = Map[UUID, ClientSession]()
    def act() {
        println("Starting gripe server on port: " + port)
        alive(port)
        register(GripeServer.name, self)

        loop {
          receive {
            case Syn(id) =>
              sessions + ((id, new ClientSession(id)))
              sender ! Ack
            case Command(id, command) =>
              sender ! sessions(id).run(command)
            case Bye(id) =>
              sessions(id).close
              sessions - id
              println("client leaving")
              sender ! new Bye(id)
          }
        }
    }
}

object GripeServer {
  val name = 'gripe
  val portProperty = "com.fastsearch.gripe.port"
}
