package dash

import java.io.File
import java.util.UUID
import Config._
import jline.{ConsoleReader, History, CandidateListCompletionHandler, Completor}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeoutException

/**
 * dash's client interface. Somewhat unintuitively we create a TCP/IP server socket
 * here and get the attach agent to connect to it instead of the other way round.
 * This is done as a security measure - i.e. we don't want to have the application
 * open up a server socket.
 */
abstract class Client {
    val id = UUID.randomUUID
    val server = new ServerPeer(start, this)

    def name: String
    def getReq: Req

    def print(strs: List[String]): Unit = strs.foreach(print(_))
    def println(message: String): Unit = print(message + "\n")
    def print(message: String): Unit = print(FString(message))
    def print(resp: Resp): Unit = resp match {
      case Success(_, response) =>
        println(bold(">> " + response))
      case Error(_, exceptionClass, message, stack) =>
        println(red("ERR: " + exceptionClass) + " - " + message)
      case x =>
        println(red("ERR: unexpected response: ") + x)
    }
    def print(message: FString): Unit

    def serverAbort: Unit = {
      println(red("Lost connection! Shutting down..."))
      sys.exit(1)
    }
    private val started = new AtomicBoolean(false)
    def shutdown: Unit = { setRunning(false); sys.exit(0) }

    protected def setRunning(start: Boolean) {
      started.synchronized {
        started.set(start)
        started.notifyAll
      }
    }

    protected def start: Unit = {
        setRunning(true)
        while(started.get()) {
            getReq match {
                case command: Command => command.run(this)
                case req: ResponseRequired => {
                  server !? req match {
                    case None => print(Error(null, "", "did not get a response.", ""))
                    case Some(resp) => print(resp)
                  }
                }
            }
        }
    }

    @throws(classOf[TimeoutException])
    def waitForConnection(timeout: Long): Unit = {
      if (!started.get()) {
        started.synchronized {
          started.wait(timeout)
          if (!started.get()) throw new TimeoutException("Timed out after waiting " + timeout + "ms for jvm attachment.")
        }
      }
    }
}

trait ScriptCompletionAware {
    /**
     * Returns true if the script is deemed to be complete and ready for processing.
     * False if we expect the user to input more in.
     * The default impl always returns true.
     */
    protected def checkScriptComplete(script: String) = true
}

/**
 * A Controller that retrieves Message instances from the user's console. Uses JLine
 * to support history and tab-completion.
 */
class InteractiveClient extends Client with Completor with internal.RhinoScriptCompletionAware {
    private val remoteTabCompleter = new RemoteTabCompleter(server)
    private val tabCompleters = List(CommandTabCompleter, new DescTabCompleter(remoteTabCompleter), remoteTabCompleter)
    private lazy val console = new ConsoleReader

    def print(message: FString): Unit = console.printString(message.formatted)

    override protected def start = {
      console.setHistory(new History(new File(System.getProperty("user.home"), ".dash_history")))
      console.addCompletor(this)
      console.setCompletionHandler(new CandidateListCompletionHandler)
      println(green("dash (" + version + ")") +
""": the {{bold:D:}}ynamically {{bold:A:}}ttaching {{bold:SH:}}ell
==============================================
For help type {{bold::help:}} at the prompt.""")
      super.start
    }

    def name = "<stdin>"
    def getReq = {
      console.readLine(FString(green("dash> ")).formatted) match {
        case null => Quit
        case str => {
          new RequestParser().parseRequest(str) match {
            case Left(ParseError(message)) =>
                println(message)
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
          console.readLine(FString(green("> ")).formatted) match {
            case null => Eval(script.toString)
            case nextLine => getCompletedScript(script + "\n" + nextLine)
          }
        }
      }
    }

    import java.util.{List => JList, Collections}
    override def complete(buffer: String, cursor: Int, candidateList: JList[_]): Int = {
        val list = candidateList.asInstanceOf[JList[String]]
        if(buffer.trim.length > 0) {
            val completions: Option[(Array[String], Int)] = (for {
              tabCompleter <- tabCompleters.iterator
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

/**
 * A Controller implementation that retrieves a single Message that consists of the command
 * to run the contents of a script file.
 */
class ScriptedClient(file: File, args: Array[String]) extends Client {
    private var hasRun = false
    def print(message: FString): Unit = Console.print(message.unformatted)
    def getReq = hasRun match {
      case false =>
        hasRun = true
        new Run(file.getAbsolutePath, args)
      case true => Quit
    }
    def name = file.getName
}
