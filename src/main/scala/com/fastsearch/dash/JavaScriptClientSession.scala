package com.fastsearch.dash

import java.util.UUID
import java.io.FileReader
import javax.script.ScriptEngineManager

class JavaScriptClientSession(val id: UUID, val out: RemoteWriter) extends ClientSession {
    private val engine = ScriptEngineFactory.get
    engine.getContext.setWriter(out)

    protected def eval(command: String) = engine.eval(command)

    // TODO - send args to script
    protected def eval(script: String, args: Array[String]) = engine.eval(new FileReader(script))

    // nothing to do really
    def close = {}
}

object JavaScriptClientSessionFactory extends ClientSessionFactory {
    def apply(id: UUID, out: RemoteWriter) = new JavaScriptClientSession(id, out)
}

object ScriptEngineFactory {
  private val factory = new ScriptEngineManager
  def get = factory.getEngineByName("JavaScript")
}
