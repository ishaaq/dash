package com.fastsearch.dash

import java.io.{Writer, PrintWriter}
import scala.actors.{Actor, Exit, FJTaskScheduler2}
import scala.actors.Actor.{loop, self}
import scala.actors.remote.{Node, RemoteActor}
import scala.actors.remote.RemoteActor.select
import scala.collection.mutable.ListBuffer

class Server(port: Int)(implicit val factory: ClientSessionFactory) extends Actor {
    trapExit = true

    override def scheduler = new FJTaskScheduler2 {
        setDaemon(true)
    }

    def act() {
        val client = select(Node("127.0.0.1", port), Constants.actorName)
        link(client)
        println("Connected to dash client on port: " + port)
        client ! Ack
        val session = factory(new RemoteWriter)

        loop {
          receive {
            case Eval(eval) =>
              sender ! session.run(eval)
            case Run(script, args) =>
              sender ! session.runScript(script, args)
            case TabCompletionRequest(prefix) =>
              sender ! session.tabCompletion(prefix)
            case Bye =>
              session.close
              println("client leaving")
              sender ! Bye
              exit
            case Exit(from, _) =>
              session.close
              println("client exiting")
              exit
          }
        }
    }
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
