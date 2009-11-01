package dash

import java.util.UUID
import Config._

sealed trait Message

import org.apache.mina.filter.reqres.{Request => MRequest}
class Request(req: ResponseRequired) extends MRequest(req.id, req, Config.requestTimeout) {
  val id = getId.asInstanceOf[UUID]
}

sealed abstract case class Req() extends Message

trait ResponseRequired extends Req {
  val id = UUID.randomUUID
}

case class TabCompletionRequest(prefix: String) extends ResponseRequired
case class Run(filePath: String, args: Array[String]) extends ResponseRequired
case class Eval(command: String) extends ResponseRequired

sealed abstract case class Command(val aliases: List[String]) extends Req {
  def this(alias: String) = this(List(alias))
  def run(client: Client): Unit = client.err.println("Not implemented yet!")
  def help: String = red("Not implemented yet!")
}
case class Help(command: String) extends Command("help") {
  def this() = this(null)

  private val helpList = List[Command](this, Reset, Quit)
  private val helpMap: Map[String, Command] = Map[String, Command]() ++ List.flatten(for(cmd <- helpList) yield cmd.aliases.map((_, cmd)))

  override def run(client: Client) = {
    val println = client.out.println _
    command match {
        case null => {
            println("EITHER run valid javascript code (which will be executed on the remote app's JVM):")
            println(green("dash> ") + "println('hello dash world!')\n")
            println("OR execute a dash command.")
            println("Commands are run by prefixing them with a colon (:). For e.g.:")
            println(green("dash> ") + ":" + red("<command>"))
            println("Where " + red("<command>") + " is one of the following:")
            (for(cmd <- helpList) yield "    " + cmd.aliases.mkString("/")).foreach(println(_))
            println("For further help on a particular command type:")
            println(":help " + red("<command>"))
          }
        case command => {
            helpMap.get(command) match {
              case Some(command) => println(command.help)
              case None => client.err.println("No such command: " + command)
            }
        }
     }
  }

  override val help = "Displays Help content. Can be run in two modes:\n" +
      "    " + red(":help") + "\t\t - Show application help.\n" +
      "    " + red(":help <command>") + "\t - Display help for " + red("<command>")
}

case object Reset extends Command("reset") {
  override def run(client: Client) = client.resetSession
  override val help = "Resets the session. All existing javascript vars and functions in the session will be cleared."
}

case object Noop extends Command("") {
  override def run(client: Client) = {}
}

case object Quit extends Command(List("exit", "quit")) {
  override def run(client: Client) = exit
  override def help = "Shuts down the dash client. You can also shut down by pressing " + red("<CNTRL-C>") + " or " + red("<CNTRL-D>")
}

@serializable
sealed abstract case class Resp(val reqId: UUID) extends Message
case class TabCompletionList(id: UUID, list: List[String]) extends Resp(id)
case class Success(id: UUID, out: List[Output], response: String) extends Resp(id)
case class Error(id: UUID, out: List[Output], response: String) extends Resp(id)

@serializable
sealed class Output(val string: String)

case class StandardOut(str: String) extends Output(str)
case class StandardErr(str: String) extends Output(str)
