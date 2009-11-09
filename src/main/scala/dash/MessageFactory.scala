package dash

import java.io.File
import java.util.{List => JList, Collections}
import jline.{ConsoleReader, History, CandidateListCompletionHandler, Completor}
import Config._

trait MessageFactory {
    implicit def formattedString2String(fString: FormattedString): String = format(fString)
    implicit def rawString2FormattedStrig(raw: String) = new FormattedString(raw)

    def get: Req
    val out = new Decorator(System.out, format)
    protected def formatStrings = true
    private def format(fString: FormattedString) = fString.getString(formatStrings)
    private def format(string: String) = string.getString(formatStrings)
}

trait ScriptCompletionAware {
    /**
     * Returns true if the script is deemed to be complete and ready for processing.
     * False if we expect the user to input more in.
     * The default impl always returns true.
     */
    protected def checkScriptComplete(script: String) = true
}

trait InteractiveMessageFactory extends MessageFactory {
    def welcomeMessage =  green("dash (" + version + ")") +
""": the {{bold:D:}}ynamically {{bold:A:}}ttaching {{bold:SH:}}ell
==============================================
For help type {{bold::help:}} at the prompt."""
}

class InteractiveMessageFactoryImpl(server: ServerPeer) extends MessageFactory with Completor with ScriptCompletionAware with InteractiveMessageFactory {
    protected val tabCompleters = List(CommandTabCompleter, new RemoteTabCompleter(server))
    private val console = new ConsoleReader

    console.setHistory(new History(new File(System.getProperty("user.home"), ".dash_history")))
    console.addCompletor(this)
    console.setCompletionHandler(new CandidateListCompletionHandler)

    out.println(welcomeMessage)

    def get = {
      console.readLine(new FormattedString(green("dash> "))) match {
        case null => Quit
        case str => {
          new RequestParser().parseRequest(str) match {
            case Left(ParseError(message)) =>
                out.println(message)
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
          console.readLine(new FormattedString(green("> "))) match {
            case null => Eval(script.toString)
            case nextLine => getCompletedScript(script + "\n" + nextLine)
          }
        }
      }
    }

    override def complete(buffer: String, cursor: Int, candidateList: JList[_]): Int = {
        val list = candidateList.asInstanceOf[JList[String]]
        if(buffer.trim.length > 0) {
            val completions: List[String] = (for {
              tabCompleter <- tabCompleters.elements
              completions = tabCompleter.getCompletions(buffer, cursor)
              if(completions.length > 0)
            } yield completions).take(1).toList.flatten
            completions match {
              case Nil => 0
              case completions => {
                completions.foreach(list.add(_))
                Collections.sort(list)
                0
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
    def get = hasRun match {
      case false =>
        hasRun = true
        new Run(script, args)
      case true => Quit
    }
}
