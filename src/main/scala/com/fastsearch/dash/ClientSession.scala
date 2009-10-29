package com.fastsearch.dash

import java.io.{PrintWriter, File, BufferedReader, FileReader}

trait ClientSession {
    val dashHome: String
    def out: RemoteWriter

    def run(command: String): Message = tryEval(eval(command))
    def runScript(script: String, args: Array[String]): Message = tryEval(eval(script, args))

    private def tryEval(eval: => AnyRef): Message = {
      try {
          eval match {
            case null => new Success(out.getAndReset, null)
            case resp => new Success(out.getAndReset, resp.toString)
          }
      } catch {
        case err =>
          err.printStackTrace(out.printWriter)
          new Error(out.getAndReset, err.getClass + ": " + err.getMessage)
      }
    }

    /**
     * A script can be resolved from an absolute path, failing which we try to resolve it from the script dir.
     */
    def resolveScriptReader(scriptPath: String) = {
        val maybeScriptFile = new File(scriptPath)
        val file = if(maybeScriptFile.exists && maybeScriptFile.isFile) maybeScriptFile else new File(Constants.scriptDir(dashHome), scriptPath)
        new BufferedReader(new FileReader(file))
    }

    def close: Unit
    protected def eval(command: String): AnyRef
    protected def eval(script: String, args: Array[String]): AnyRef
    def tabCompletion(prefix: String): TabCompletionList
}

trait ClientSessionFactory {
    def apply(dashHome: String, out: RemoteWriter): ClientSession
}

