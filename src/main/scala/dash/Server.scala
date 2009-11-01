package dash

import java.io.{Writer, PrintWriter}
import scala.collection.mutable.ListBuffer
import java.util.UUID
import Config.clientSession

class Server(id: UUID, port: Int, dashHome: String) {
    val client = new ClientPeer(port, receive, close)
    val session = clientSession(dashHome, new RemoteWriter)

    def receive(req: Req): Unit = {
      val reqId = req match {
        case req: ResponseRequired => Some(req.id)
        case req => None
      }
      req match {
            case Eval(eval) =>
              client ! session.run(reqId.get, eval)
            case Run(script, args) =>
              client ! session.runScript(reqId.get, script, args)
            case Reset =>
              session.reset
            case TabCompletionRequest(prefix) =>
              client ! session.tabCompletion(reqId.get, prefix)
            case x => println("unexpected dash message: " + x)
      }
    }

    private def close: Unit = {
      session.close
      println("dash client left: " + id)
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
