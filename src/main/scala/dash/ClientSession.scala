package dash

import java.util.UUID
import java.io.{PrintWriter, File, BufferedReader, FileReader}

abstract class ClientSession(val dashHome: String, val out: RemoteWriter) extends ScriptEngine {
    private def tryEval(reqId: UUID, eval: => AnyRef): Message = {
      try {
          eval match {
            case null => new Success(reqId, out.getAndReset, null)
            case resp => new Success(reqId, out.getAndReset, resp.toString)
          }
      } catch {
        case err =>
          err.printStackTrace(out.printWriter)
          new Error(reqId, out.getAndReset, err.getClass + ": " + err.getMessage)
      }
    }

    /**
     * A script can be resolved from an absolute path, failing which we try to resolve it from the script dir.
     */
    def resolveScriptReader(scriptPath: String) = {
        val maybeScriptFile = new File(scriptPath)
        val file = if(maybeScriptFile.exists && maybeScriptFile.isFile) maybeScriptFile else new File(Config.scriptDir(dashHome), scriptPath)
        new BufferedReader(new FileReader(file))
    }

    def run(reqId: UUID, command: String): Message = tryEval(reqId, eval(command))
    def runScript(reqId: UUID, script: String, args: Array[String]): Message = tryEval(reqId, eval(script, args))
}