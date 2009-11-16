package dash

import java.io.File
import java.util.{List => JList, Collections}
import jline.{ConsoleReader, History, CandidateListCompletionHandler, Completor}
import Config._

/**
 * The Client uses a MessageFactory implementation to retrieve Message instances from
 * the user.
 */
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

/**
 * A MessageFactory that retrieves Message instances from the user's console. Uses JLine
 * to support history and tab-completion.
 */
class InteractiveMessageFactoryImpl(server: ServerPeer) extends MessageFactory with Completor with ScriptCompletionAware with InteractiveMessageFactory {
    private val remoteTabCompleter = new RemoteTabCompleter(server)
    private val tabCompleters = List(CommandTabCompleter, new DescTabCompleter(remoteTabCompleter), remoteTabCompleter)
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
            val completions: Option[(Array[String], Int)] = (for {
              tabCompleter <- tabCompleters.elements
              (completions, offset) = tabCompleter.getCompletions(buffer, cursor)
            } yield (completions, offset)).find(x => { x._1.size > 0 })
            completions match {
              case None => 0
              case Some((matches, offset)) => {
                matches.foreach(list.add(_))
                Collections.sort(list)
                offset
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

/**
 * A MessageFactory implementation that retrieves a single Message that consists of the command
 * to run the contents of a script file.
 */
class ScriptMessageFactory(script: String, args: Array[String]) extends MessageFactory {
    private var hasRun = false
    def get = hasRun match {
      case false =>
        hasRun = true
        new Run(script, args)
      case true => Quit
    }
}
