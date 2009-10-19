package com.fastsearch.dash

import java.util.UUID
import java.io.Writer
import scala.actors.Actor
import scala.actors.Actor._
import scala.actors.OutputChannel
import scala.actors.remote.RemoteActor
import scala.actors.remote.RemoteActor._
import scala.collection.mutable.Map

class Server(port: Int)(implicit val factory: ClientSessionFactory) extends Actor {
    private val sessions = Map[UUID, ClientSession]()
    def act() {
        println("Starting dash server on port: " + port)
        alive(port)
        register(Server.name, self)

        loop {
          receive {
            case Syn(id) =>
              sessions + ((id, factory(id, new RemoteWriter(sender))))
              sender ! Ack
            case Command(id, command) =>
              sender ! sessions(id).run(command)
            case Run(id, script, args) =>
              sender ! sessions(id).runScript(script, args)
            case Bye(id) =>
              sessions(id).close
              sessions - id
              println("client leaving")
              sender ! new Bye(id)
          }
        }
    }
}

object Server {
  val name = 'dash
  val portProperty = "com.fastsearch.dash.port"
}

class RemoteWriter(sender: OutputChannel[Message]) extends Writer {
    def close() = {}
    def flush() = {}
    def write(chars: Array[Char], offset: Int, length: Int) = {}

    def print(str: String) = sender ! Print(str)
    def println(str: String) = sender ! Print(str + "\n")
}
