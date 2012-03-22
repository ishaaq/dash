package dash
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import scala.collection.mutable.ListBuffer
import java.util.concurrent.ExecutionException

/**
 * Exposes a programmable interface for Dash.
 */
class DashAPI private (pid: String, timeout: Long = 0, scriptPath: Option[String] = None) {
  private val attacher = Attacher(Some(pid))
  private val pipe = new APIPipe
  attacher.attach(new ProgrammableClient(pipe), timeout, scriptPath).foreach { error => throw new RuntimeException(error) }

  def detach() {
    pipe.runCommand(":" + Quit.aliases.head)
    pipe.stop
  }

  def run(command: String): Array[String] = pipe.runCommand(command)
}


private class APIPipe {
  private val commandQ = new ArrayBlockingQueue[String](1)
  private val resultQ = new ArrayBlockingQueue[Boolean](1)
  private val strings = ListBuffer[String]()
  private val timeout = 500L

  @volatile private var running = true
  def pollCommand: String = poll(commandQ).getOrElse(null)

  def runCommand(command: String): Array[String] = {
    offer(commandQ, command) match {
      case true => {
        val result = poll(resultQ)
        val strs = strings.toArray
        strings.clear
        result match {
          case Some(true) => strs
          case Some(false) => throw new RuntimeException(strs.mkString("\n"));
          case None => Array[String]()
        }
      }
      case false => throw new RuntimeException("Could not run command - has the vm been killed?");
    }
  }

  def addString(string: String): Unit = strings.append(string)

  def resultInput(result: String, success: Boolean): Unit = {
    addString(result)
    offer(resultQ, success)
  }

  private def offer[T](q: BlockingQueue[T], v: T): Boolean = {
    while (running) {
      if (q.offer(v, timeout, TimeUnit.MILLISECONDS)) {
        return true
      }
    }
    return false
  }

  private def poll[T](q: BlockingQueue[T]): Option[T] = {
    while (running) {
      val result = q.poll(timeout, TimeUnit.MILLISECONDS)
      if (result != null) return Some(result)
    }
    return None
  }

  def stop: Unit = running = false
}

private class ProgrammableClient(pipe: APIPipe) extends Client {
  def getReq = {
    pipe.pollCommand match {
      case null => Quit
      case str => {
        new RequestParser().parseRequest(str) match {
          case Left(ParseError(message)) =>
              println(message)
              Noop
          case Right(req) => req
        }
      }
    }
  }

  override def print(resp: Resp): Unit = resp match {
    case Success(_, response) =>
      pipe.resultInput(FString(response).unformatted, true)
    case Error(_, exceptionClass, message, stack) =>
      pipe.resultInput("ERR: " + exceptionClass + " - " + message, false)
    case x =>
      pipe.resultInput("ERR: unexpected response: " + x, false)
  }

  override def serverAbort {
    Console.err.println("Lost connection! Shutting client down...")
    pipe.stop
  }

  override def shutdown: Unit = {
   setRunning(false)
   pipe.stop
   server.shutdown
  }

  def print(message: FString): Unit = pipe.addString(message.unformatted)

  def name = "<api>"
}

object DashAPI {
  def getAttachableVms: Array[Vm] = VmUtil.getAttachableVms.toArray

  def attach(vmId: String): DashAPI = new DashAPI(vmId)
  def attach(vmId: String, scriptPath: String): DashAPI = new DashAPI(vmId, 0, Option(scriptPath))
  def attach(vmId: String, timeout: Long, scriptPath: String): DashAPI = new DashAPI(vmId, timeout, Option(scriptPath))
}
