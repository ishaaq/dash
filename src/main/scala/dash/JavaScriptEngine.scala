package dash

import java.util.UUID
import Config._
import javax.script.{ScriptEngineManager, ScriptContext}
import scala.collection.jcl.Conversions.convertSet
import scala.io.Source
import _root_.sun.org.mozilla.javascript.internal.NativeArray

trait JavaScriptEngine extends ScriptEngine {
    this: ClientSession =>

    private val argStr = "arguments"
    private val shellArg = "__shell__"

    private val globalsFilter = Set(shellArg, "context", "desc")

    private var wrapper = new EngineWrapper(out)

    private def engine = wrapper.engine
    private def bindings = wrapper.bindings


    private val predef = Source.fromInputStream(getClass.getResourceAsStream("predef.js")).getLines.mkString("")

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
      engine.eval(predef)
    }

    protected def eval(command: String) = {
        createLoadFn
        wrapper.engine.eval(command)
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

    def tabCompletion(id: UUID, prefix: String) = {
      createLoadFn
      val trimmed = prefix.trim
      new TabCompletionList(id, bindings.keySet.filter(_.startsWith(trimmed)).toList)
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

    def reset = bindings.clear

    def close = {
      reset
      wrapper = null
    }

    def describe(ref: String) = {
      createLoadFn
      val descArgs = ref match {
        case "" => {
            val globals = bindings.keySet.filter(key => !globalsFilter.contains(key))
            globals.mkString("','")
        }
        case ref => ref
      }
      try {
          eval("desc('" + descArgs + "')")
          Right(out.getAndReset)
      } catch {
        case e => Left("An error occurred: " + e.getMessage)
      }
    }
}

class EngineWrapper(out: RemoteWriter) {
    val engine = new ScriptEngineManager().getEngineByName("JavaScript")
    def ctxt = engine.getContext
    def bindings = ctxt.getBindings(ScriptContext.ENGINE_SCOPE)
    ctxt.setWriter(out)
}
