package dash

import java.io.File
import java.util.{List => JList, Collections}
import jline.{ConsoleReader, History, CandidateListCompletionHandler, Completor}
import Config._

trait MessageFactory {
    def get: Req
    def out: Printer
    def err: Printer
}

trait ScriptCompletionAware {
    /**
     * Returns true if the script is deemed to be complete and ready for processing.
     * False if we expect the user to input more in.
     * The default impl always returns true.
     */
    protected def checkScriptComplete(script: String) = true
}

class InteractiveMessageFactory(server: ServerPeer) extends MessageFactory with Completor with ScriptCompletionAware {
    private val console = new ConsoleReader
    val out = System.out
    val err = new Decorator(System.err, red)

    console.setHistory(new History(new File(System.getProperty("user.home"), ".dash_history")))
    console.addCompletor(this)
    console.setCompletionHandler(new CandidateListCompletionHandler)

    println(
      red("dash (" + version + ")") + ": the Dynamically Attaching SHell\n" +
      "==============================================\n" +
      "For help type " + red(":help") + " at the prompt."
    )

    def get = {
      console.readLine(green("dash> ")) match {
        case null => Quit
        case str => {
          new RequestParser().parseRequest(str) match {
            case Left(ParseError(message)) =>
                err.println(message)
                Noop
            case Right(req) => {
                req match {
                  case Eval(str) => getCompletedScript(str)
                  case _ => req
                }
            }
          }
        }
      }
    }

    private def getCompletedScript(script: String): Eval = {
      checkScriptComplete(script) match {
        case true =>  Eval(script.toString)
        case false => {
          console.readLine(green("> ")) match {
            case null => Eval(script.toString)
            case nextLine => getCompletedScript(script + "\n" + nextLine)
          }
        }
      }
    }

    override def complete(buffer: String, cursor: Int, candidateList: JList[_]): Int = {
        val list = candidateList.asInstanceOf[JList[String]]
        if(buffer.trim.length > 0) {
            server !? (new TabCompletionRequest(buffer)) match {
              case None => buffer.length
              case Some(x) => x match {
                case TabCompletionList(_, completionList) => {
                  completionList.foreach(list.add(_))
                  Collections.sort(list)
                  0
                }
                case x => {
                  err.println("Invalid response: " + x)
                  0
                }
              }
            }
        } else {
            buffer.length
        }
    }
}

class Decorator(actual: Printer, decorator: String => String) {
    def print(str: String) = actual.print(decorator(str))
    def println(str: String) = actual.println(decorator(str))
}

class ScriptMessageFactory(script: String, args: Array[String]) extends MessageFactory {
    private var hasRun = false
    val out = System.out
    val err = System.err
    def get = hasRun match {
      case false =>
        hasRun = true
        new Run(script, args)
      case true => Quit
    }
}
