package com.fastsearch.dash

import java.util.UUID
import java.io.Writer
import scala.actors.Actor
import scala.actors.Actor._
import scala.actors.OutputChannel
import scala.actors.remote.RemoteActor
import scala.actors.remote.RemoteActor._
import scala.collection.mutable.Map

class Server(port: Int) extends Actor {
    private val sessions = Map[UUID, ClientSession]()
    def act() {
        println("Starting dash server on port: " + port)
        alive(port)
        register(Server.name, self)

        loop {
          receive {
            case Syn(id) =>
              sessions + ((id, new ClientSession(id, new RemoteWriter(sender))))
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
    private val sb = new StringBuilder
    def close() = {
        if(sb.size > 0) {
          flush
        }
    }
    def flush() = {
        sender ! Print(sb.toString)
        sb.clear
    }
    def write(chars: Array[Char], offset: Int, length: Int) = sb.append(chars, offset, length)

    def print(str: String) = sb.append(str)
}
