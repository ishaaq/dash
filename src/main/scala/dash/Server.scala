package dash

import java.util.UUID
import Config._

/**
 * dash's server interface. Somewhat unintuitively we create a TCP/IP client socket
 * here and connect to the Client from here instead of the other way round.
 * This is done as a security measure - i.e. we don't want to have the application
 * open up a server socket.
 */
class Server(id: UUID, port: Int, dashHome: String, stdinName: String) {
    val out = new RemoteWriter(this)
    val session = clientSession(dashHome, out, stdinName)
    val client = new ClientPeer(port, receive, close)

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
            case TabCompletionRequest(prefix, cursor) =>
              client ! session.tabCompletion(reqId.get, prefix, cursor)
            case Desc(jsRoot) => {
              client ! new Description(reqId.get, session.describe(jsRoot))
            }
            case x => println("unexpected dash message: " + x)
      }
    }

    private def close: Unit = {
      session.close
      println("dash client left: " + id)
    }
}

import java.io.{Writer, PrintWriter}
import scala.collection.mutable.ListBuffer
class RemoteWriter(server: Server) extends Writer {
    private lazy val client = server.client
    private val sb = new StringBuilder
    private val printWriter = new PrintWriter(this, true)

    def close = flush
    def flush = {
      if(sb.length > 0) {
          client ! Print(sb.toString)
          sb.clear
       }
    }

    def write(chars: Array[Char], offset: Int, length: Int) = sb.appendAll(chars, offset, length)

    def print(str: String) = client ! Print(str)
    def println(str: String) = client ! Print(str + "\n")

    val help = "A custom implementation of java.io.Writer that outputs remotely to the dash client's console."
}
