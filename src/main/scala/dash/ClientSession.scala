package dash

import java.util.UUID
import java.io.{PrintWriter, StringWriter, File}

abstract class ClientSession(val scriptsDir: String, val out: RemoteWriter, val stdinName: String) extends ScriptEngine {
    private def tryEval(reqId: UUID, eval: => AnyRef): Message = {
      try {
          eval match {
            case null => new Success(reqId, null)
            case resp => new Success(reqId, resp.toString)
          }
      } catch {
        case err: Throwable => {
          val sw = new StringWriter
          err.printStackTrace(new PrintWriter(sw))
          new Error(reqId, err.getClass.getName, err.getMessage, sw.toString)
        }
      }
    }

    /**
     * A script can be resolved from an absolute path, failing which we try to resolve it from the script dir.
     */
    def resolveScriptFile(scriptPath: String): File = {
        val maybeScriptFile = new File(scriptPath)
        if(maybeScriptFile.exists && maybeScriptFile.isFile) maybeScriptFile else new File(scriptsDir, scriptPath)
    }

    def run(reqId: UUID, command: String): Message = tryEval(reqId, eval(command))
    def runScript(reqId: UUID, script: String, args: Array[String]): Message = tryEval(reqId, eval(script, args))
}
