package dash

import java.util.UUID
import Config._

/**
 * The dash Client and Server instances communicate via message-passing. All messages
 * implement this trait.
 */
sealed trait Message


import org.apache.mina.filter.reqres.{Request => MRequest}
/**
 * The Request is an extension of the Apache MINA Request class that encapsulates a
 * ResponseRequired message. MINA will take care of matching the request and the response
 * via the request id.
 */
class Request(req: ResponseRequired) extends MRequest(req.id, req, 0) {
  val id = getId.asInstanceOf[UUID]
}

sealed abstract case class Req() extends Message

trait ResponseRequired extends Req {
  val id = UUID.randomUUID
}

case class TabCompletionRequest(prefix: String, cursor: Int) extends ResponseRequired
case class Run(filePath: String, args: Array[String]) extends ResponseRequired
case class Eval(command: String) extends ResponseRequired

sealed abstract case class Command(val aliases: List[String]) extends Req {
  def this(alias: String) = this(List(alias))
  def run(client: Client): Unit = client.out.println(red("Not implemented yet!"))
  def help: String = red("Not implemented yet!")
}

object Help {
  val helpList = List[Command](new Help(""), new Desc(), Reset, Quit)
  val helpMap: Map[String, Command] = Map[String, Command]() ++ List.flatten(for(cmd <- helpList) yield cmd.aliases.map((_, cmd)))
}

case class Help(command: String) extends Command("help") {
  def this() = this(null)

  override def run(client: Client) = {
    import Help.{helpList, helpMap}
    val println = client.out.println _
    command match {
        case null => {
            println("""EITHER run valid javascript code (which will be executed on the remote app's JVM):
{{green:dash>:}} println('hello dash world!')
OR execute a dash command.
Commands are run by prefixing them with a colon (:). For e.g.:
{{green:dash>:}} :{{bold:<command>:}}
Where {{bold:<command>:}} is one of the following:""")
            (for(cmd <- helpList) yield "    " + cmd.aliases.mkString("/")).foreach(println(_))
            println("For further help on a particular command type:")
            println(":help {{bold:<command>:}}")
          }
        case command => {
            helpMap.get(command) match {
              case Some(command) => println(command.help)
              case None => client.out.println(red("No such command: ") + command)
            }
        }
     }
  }

  override val help = """Displays Help content. Can be run in two modes:
    {{bold::help:}}           - Show application help.
    {{bold::help <command>:}} - Display help for {{bold:<command>:}}."""
}

case object Reset extends Command("reset") {
  override def run(client: Client) = client.server ! this
  override val help = "Resets the session. All existing javascript vars and functions in the session will be cleared."
}

case class Desc(jsRoot: String) extends Command("desc") with ResponseRequired {
  def this() = this("")
  override def run(client: Client) = {
    client.server !? this match {
      case Some(Description(_, error)) => error match {
        case Some(error) => client.print(List(error))
        case None =>
      }
      case x => client.out.println(red("Unexpected response: ") + x)
    }
  }
  override val help ="""Describes the contents of a javascript variable reference in session.
The variable may refer to a primitive, a function or an object.
Can be run in two modes:
    {{bold::desc:}}       - Describes the global variables in the session.
    {{bold::desc <var>:}} - Describes the javascript primitive/function/object specified by {{bold:<var>:}}."""
}

case object Noop extends Command("") {
  override def run(client: Client) = {}
}

case object Quit extends Command(List("exit", "quit")) {
  override def run(client: Client) = exit
  override def help = "Shuts down the dash client. You can also shut down by pressing {{bold:<CNTRL-C>:}} or {{bold:<CNTRL-D>:}}."
}

@serializable
sealed abstract case class Resp(val reqId: UUID) extends Message
case class TabCompletionList(id: UUID, list: List[String]) extends Resp(id)
case class Success(id: UUID, response: String) extends Resp(id)
case class Error(id: UUID, errorClass: String, message: String, stack: String) extends Resp(id)
case class Description(id: UUID, error: Option[String]) extends Resp(id)

case class Print(string: String) extends Message
