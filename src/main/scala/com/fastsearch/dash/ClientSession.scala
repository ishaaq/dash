package com.fastsearch.dash

import java.util.UUID
import javax.script._

class ClientSession(id: UUID, out: RemoteWriter) {
    private val engine = ScriptEngineFactory.get
          engine.getContext.setWriter(out)

    def run(command: String): Message = {
      try {
          engine.eval(command) match {
            case null => new Response(null)
            case x => new Response(x.toString)
          }
      } catch {
        case e => new ErrorResponse(e.getMessage)
      }
    }

    def close = {
      // nothing to do really
    }
}

object ScriptEngineFactory {
  private val factory = new ScriptEngineManager
  def get = factory.getEngineByName("JavaScript")
}


