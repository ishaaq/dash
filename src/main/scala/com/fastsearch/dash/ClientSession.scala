package com.fastsearch.dash

import java.util.UUID
import java.io.PrintWriter

trait ClientSession {
    def id: UUID
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

    def close: Unit
    protected def eval(command: String): AnyRef
    protected def eval(script: String, args: Array[String]): AnyRef
    def tabCompletion(prefix: String): TabCompletionList
}

trait ClientSessionFactory {
    def apply(id: UUID, out: RemoteWriter): ClientSession
}

