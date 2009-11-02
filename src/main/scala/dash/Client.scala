package dash

import java.io.File
import java.util.UUID
import Config._

class Client(id: UUID, file: Option[File], args: Array[String]) {
    private lazy val s = new ServerPeer(start, out, err)
    def server = s
    def port = server.port

    private lazy val messageFactory = file match {
                            case None => new InteractiveMessageFactory(server)
                            case Some(script) => new ScriptedMessageFactory(script.getAbsolutePath, args)
                          }
    lazy val out: Printer = messageFactory.out
    lazy val err: Printer = messageFactory.err

    def print(outs: List[Output]): Unit = outs.foreach ( _ match {
      case StandardOut(str) => out.print(str)
      case StandardErr(str) => err.print(str)
    })

    private def start: Unit = {
        val processResponse: PartialFunction[Resp, Unit] = {
          case Success(_, outs, response) =>
            print(outs)
            out.println(">> " + response)
          case Error(_, outs, response) =>
            print(outs)
            err.println("ERR! " + response)
          case x => err.println("unexpected response: " + x)
        }

        while(true) {
            messageFactory.get match {
                case command: Command => command.run(this)
                case req: ResponseRequired => {
                  server !? req match {
                    case None => err.println("ERR! did not get a response")
                    case Some(resp) => processResponse(resp)
                  }
                }
                case req: Req => server ! req
            }
        }
    }
}
