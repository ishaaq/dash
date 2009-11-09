package dash

import ARM._
import java.util.UUID
import java.io.FileReader
import Config._
import javax.script.{ScriptEngineManager, ScriptContext}
import scala.collection.jcl.Conversions.convertSet
import scala.io.Source
import _root_.sun.org.mozilla.javascript.internal.NativeArray

trait JavaScriptEngine extends ScriptEngine {
    this: ClientSession =>

    private val argStr = "arguments"
    private val __shell__ = "__shell__"
    private val __desc__ = "__desc__"

    private val globalsFilter = Set(__shell__, __desc__, "context")

    private var wrapper = new EngineWrapper(out)

    private def engine = wrapper.engine
    private def bindings = wrapper.bindings


    private val predef = Source.fromInputStream(getClass.getResourceAsStream("predef.js")).getLines.mkString("")

    private def evalString(string: String) = engine.eval(string)
    private def evalScript(script: String) = {
          withCloseable(new FileReader(resolveScriptFile(script))) {
            reader => engine.eval(reader)
          }
    }

    private def runEval(evaluator: => AnyRef, doLoadPredef: Boolean) = {
        if(doLoadPredef) doLoadPredef
        evaluator
    }

    /**
     * Creates a special 'load' function to be able to load scripts. This fn is called
     * once before every eval() call. This is so that if another script
     * inadvertantly redefines the load function then we don't put ourselves in a hole
     * out of which we can't get out. Not sure if this is exactly what they've done,
     * but, on the face of it, it looks like Sun has done the same thing for
     * the 'print' and 'println' functions, i.e. you cannot redefine them.
     */
    private def doLoadPredef: Unit = {
      bindings.put(__shell__, this)
      runEval(evalString(predef), false)
    }

    protected def eval(command: String) = runEval(evalString(command), true)

    protected def eval(script: String, args: Array[String]) = {
      val oldArgs = if(bindings.containsKey(argStr)) Some(bindings.get(argStr)) else None
      try {
          engine.put(argStr, args)
          runEval(evalScript(script), true)
      } finally {
          if(!oldArgs.isEmpty) bindings.put(argStr, oldArgs.get)
      }
    }

    def run(script: String, args: NativeArray): AnyRef = eval(script, convertArgs(args))

    def tabCompletion(id: UUID, prefix: String, cursor: Int) = {
      doLoadPredef
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
      doLoadPredef
      val descArgs = ref match {
        case "" => {
            val globals = bindings.keySet.filter(key => !globalsFilter.contains(key))
            globals.mkString("','")
        }
        case ref => ref
      }
      try {
          runEval(evalString(__desc__ + "('" + descArgs + "')"), false)
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
