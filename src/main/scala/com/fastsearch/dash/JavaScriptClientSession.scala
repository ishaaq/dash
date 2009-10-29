package com.fastsearch.dash

import javax.script.{ScriptEngineManager, ScriptContext}
import scala.collection.jcl.Conversions.convertSet
import _root_.sun.org.mozilla.javascript.internal.NativeArray

class JavaScriptClientSession(val dashHome: String, val out: RemoteWriter) extends ClientSession {
    private val argStr = "arguments"
    private val shellArg = "__shell__"

    private val engine = ScriptEngineFactory.get
    private val ctxt = engine.getContext
    private val bindings = ctxt.getBindings(ScriptContext.ENGINE_SCOPE)
    ctxt.setWriter(out)

    /**
     * Creates a special 'load' function to be able to load scripts. This fn is called
     * once before every eval() call. This is so that if another script
     * inadvertantly redefines the load function then we don't put ourselves in a hole
     * out of which we can't get out. Not sure if this is exactly what they've done,
     * but, on the face of it, it looks like Sun has done the same thing for
     * the 'print' and 'println' functions, i.e. you cannot redefine them.
     */
    private def createLoadFn:Unit = {
      bindings.put(shellArg, this)
      engine.eval("""load = function() {
         var scriptFile = arguments[0]
         var args = arguments.length > 1 ? Array.prototype.slice.call(arguments, 1) : []
         """ + shellArg + """.run(scriptFile, args)
         }""")
    }

    protected def eval(command: String) = {
        createLoadFn
        engine.eval(command)
    }

    protected def eval(script: String, args: Array[String]) = {
      createLoadFn
      val oldArgs = if(bindings.containsKey(argStr)) Some(bindings.get(argStr)) else None
      try {
          engine.put(argStr, args)
          engine.eval(resolveScriptReader(script))
      } finally {
          if(!oldArgs.isEmpty) bindings.put(argStr, oldArgs.get)
      }
    }

    def run(script: String, args: NativeArray): AnyRef = eval(script, convertArgs(args))

    def tabCompletion(prefix: String) = {
      val trimmed = prefix.trim
      new TabCompletionList(bindings.keySet.filter(_.startsWith(trimmed)).toList)
    }

    private def convertArgs(jsArr: NativeArray): Array[String] = {
      val arr = new Array[String](jsArr.getLength.asInstanceOf[Int])
      jsArr.getIds.foreach { x =>
        val indx = x.asInstanceOf[Int]
        arr(indx) = jsArr.get(indx, null) match {
          case null => null
          case str: String => str
          case x => x.toString
        }
      }
      arr
    }

    // nothing to do really
    def close = {}
}

object JavaScriptClientSessionFactory extends ClientSessionFactory {
    def apply(dashHome: String, out: RemoteWriter) = new JavaScriptClientSession(dashHome, out)
}

object ScriptEngineFactory {
  private val factory = new ScriptEngineManager
  def get = factory.getEngineByName("JavaScript")
}
