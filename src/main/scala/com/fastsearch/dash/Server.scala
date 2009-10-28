package com.fastsearch.dash

import java.util.UUID
import java.io.{Writer, PrintWriter}
import scala.actors.{Actor, FJTaskScheduler2}
import scala.actors.Actor.{loop, self}
import scala.actors.OutputChannel
import scala.actors.remote.RemoteActor
import scala.actors.remote.RemoteActor.{alive, register}
import scala.collection.mutable.{Map, ListBuffer}

class Server(port: Int)(implicit val factory: ClientSessionFactory) extends Actor {
    private val sessions = Map[UUID, ClientSession]()

    override def scheduler = DaemonScheduler

    def act() {
        println("Starting dash server on port: " + port)
        alive(port)
        register(Constants.actorName, self)

        loop {
          react {
            case Syn(id) =>
              println("client entered: " + id)
              sessions + ((id, factory(id, new RemoteWriter)))
              sender ! Ack
            case Eval(id, eval) =>
              sender ! sessions(id).run(eval)
            case Run(id, script, args) =>
              sender ! sessions(id).runScript(script, args)
            case TabCompletionRequest(id, prefix) =>
              sender ! sessions(id).tabCompletion(prefix)
            case Bye(id) =>
              sessions(id).close
              sessions - id
              println("client leaving")
          }
        }
    }
}

object DaemonScheduler extends FJTaskScheduler2 {
    setDaemon(true)
}

class RemoteWriter extends Writer {
    private val sb = new StringBuilder
    private val buffer = new ListBuffer[Output]

    val printWriter = new PrintWriter(this, true)
    def close = flush
    def flush = {
      if(sb.length > 0) {
          append(sb.toString)
          sb.clear
       }
    }

    def write(chars: Array[Char], offset: Int, length: Int) = sb.append(chars, offset, length)

    def print(str: String) = append(str)
    def println(str: String) = append(str + "\n")

    private def append(str: String) = buffer += new StandardOut(str)

    def getAndReset = {
      val list = buffer.toList
      buffer.clear
      list
    }
}
