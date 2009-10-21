package com.fastsearch.dash

import java.util.UUID
import java.io.{FileReader, File}
import org.jruby.embed.{ScriptingContainer, LocalVariableBehavior, LocalContextScope}

class JRubyClientSession(val id: UUID, val out: RemoteWriter) extends ClientSession {
    private val container = new ScriptingContainer(LocalContextScope.SINGLETHREAD, LocalVariableBehavior.PERSISTENT)
    container.setWriter(out)
    val bootstrap = new File(Constants.dashHome, "scripts/bootstrap.rb")
    container.runScriptlet(new FileReader(bootstrap), bootstrap.getAbsolutePath)

    protected def eval(command: String) = container.runScriptlet(command)

    // TODO - send args to script
    protected def eval(script: String, args: Array[String]) = container.runScriptlet(new FileReader(script), script)

    // TODO - tab completion
    def tabCompletion(prefix: String) = new TabCompletionList(List[String]())

    // nothing to do really
    def close = {}
}

object JRubyClientSessionFactory extends ClientSessionFactory {
    def apply(id: UUID, out: RemoteWriter) = new JRubyClientSession(id, out)
}
