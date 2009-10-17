package com.fastsearch.dash

import java.io.{BufferedReader, File, FileReader}
import java.util.UUID
import javax.script._

class ClientSession(id: UUID, out: RemoteWriter) {
    private val engine = ScriptEngineFactory.get
          engine.getContext.setWriter(out)

    def run(command: String): Message = tryRun(engine.eval(command))

    // todo work with args as well:
    def runScript(script: String, args: Array[String]): Message = tryRun(engine.eval(new FileReader(script)))

    private def tryRun(eval: => Any): Message = {
      try {
          eval match {
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


