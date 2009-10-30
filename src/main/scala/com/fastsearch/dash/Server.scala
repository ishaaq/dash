package com.fastsearch.dash

import java.io.{Writer, PrintWriter}
import scala.collection.mutable.ListBuffer
import java.util.UUID

class Server(id: UUID, port: Int, dashHome: String)(implicit val factory: ClientSessionFactory) {
    val client = new ClientPeer(port, receive)
    val session = factory(dashHome, new RemoteWriter)

    def receive(req: Req): Unit = {
      val reqId = req.id
      req match {
            case Eval(eval) =>
              client ! session.run(reqId, eval)
            case Run(script, args) =>
              client ! session.runScript(reqId, script, args)
            case TabCompletionRequest(prefix) =>
              client ! session.tabCompletion(reqId, prefix)
            case Bye() =>
              session.close
              println("dash client leaving: " + id)
            case x => println("unexpected message: " + x)
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
