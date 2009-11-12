package dash

import java.io.File
import java.util.UUID
import Config._

/**
 * dash's client interface. Somewhat unintuitively we create a TCP/IP server socket
 * here and get the attach agent to connect to it instead of the other way round.
 * This is done as a security measure - i.e. we don't want to have the application
 * open up a server socket.
 */
class Client(id: UUID, file: Option[File], args: Array[String]) {
    private lazy val s = new ServerPeer(start, out)
    def server = s
    def port = server.port

    private lazy val messageFactory: MessageFactory = Config.messageFactory(file, args, server)
    lazy val out: Printer = messageFactory.out

    def print(outs: List[String]): Unit = outs.foreach(out.print(_))

    private def start: Unit = {
        val processResponse: PartialFunction[Resp, Unit] = {
          case Success(_, outs, response) =>
            print(outs)
            out.println(bold(">> " + response))
          case Error(_, exceptionClass, message, stack) =>
            out.println(red("ERR: " + exceptionClass) + " - " + message)
          case x => out.println(red("unexpected response: ") + x)
        }

        while(true) {
            messageFactory.get match {
                case command: Command => command.run(this)
                case req: ResponseRequired => {
                  server !? req match {
                    case None => out.println(red("ERR: ") + "did not get a response.")
                    case Some(resp) => processResponse(resp)
                  }
                }
                case req: Req => server ! req
            }
        }
    }
}
