package com.fastsearch.dash

import java.util.UUID

trait ClientSession {
    def id: UUID
    def out: RemoteWriter

    def run(command: String): Message = tryEval(eval(command))

    // todo work with args as well:
    def runScript(script: String, args: Array[String]): Message = tryEval(eval(script, args))

    private def tryEvaluation(eval: => Any): Message = {
      try {
          eval match {
            case null => new Response(null)
            case x => new Response(x.toString)
          }
      } catch {
        case e => new ErrorResponse(e.getMessage)
      }
    }


    private def tryEval(eval: => Any): Message = {
      try {
          eval match {
            case null => new Response(null)
            case x => new Response(x.toString)
          }
      } catch {
        case e => new ErrorResponse(e.getMessage)
      }
    }

    def close: Unit
    protected def eval(command: String): AnyRef
    protected def eval(script: String, args: Array[String]): AnyRef
}

trait ClientSessionFactory {
    def apply(id: UUID, out: RemoteWriter): ClientSession
}

