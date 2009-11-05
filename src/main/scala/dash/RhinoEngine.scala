package dash

import ARM._
import java.util.UUID
import java.io.FileReader
import Config._
import org.mozilla.javascript.NativeArray

trait RhinoEngine extends ScriptEngine {
  this: ClientSession =>

    private val argStr = "arguments"
    private val __desc__ = "__desc__"

    private var engine = new Engine(out)

    def tabCompletion(id: UUID, prefix: String) = {
      val trimmed = prefix.trim

      new TabCompletionList(id, engine.getPropertyIds.filter(_.startsWith(trimmed)).toList)
    }

    def reset = engine = new Engine(out)

    def close = engine = null

    protected def eval(command: String) = engine.run(command)
    protected def eval(script: String, args: Array[String]): AnyRef = {
      val oldArgs = if(engine.hasProperty(argStr)) Some(engine.getProperty(argStr)) else None
      try {
          engine.setProperty(argStr, args)
          engine.run(new FileReader(resolveScriptFile(script)), script)
      } finally {
          oldArgs match {
            case None => engine.deleteProperty(argStr)
            case Some(oldArgs) => engine.setProperty(argStr, oldArgs)
          }
      }
    }

    def run(script: String, args: NativeArray): AnyRef = eval(script, args)

    private implicit def nativeArr2StringArr(jsArr: NativeArray): Array[String] = {
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

    def describe(ref: String) = {
      val descArgs = ref match {
        case "" => engine.getPropertyIds.mkString("','")
        case ref => ref
      }
      try {
          engine.run(__desc__ + "('" + descArgs + "')")
          Right(out.getAndReset)
      } catch {
        case e => Left("An error occurred: " + e.getMessage)
      }
    }

    import org.mozilla.javascript.ScriptableObject.{DONTENUM, PERMANENT, READONLY}
    import org.mozilla.javascript.{Context, NativeFunction}
    import java.io.{Reader, InputStreamReader}
    class Engine(out: RemoteWriter) extends RhinoScopeWrapper {
        withContext { cx =>
            val hiddenConst = READONLY | PERMANENT | DONTENUM
            val const = READONLY | PERMANENT
            scope.defineProperty("out", Context.javaToJS(out, scope), const)
            scope.defineProperty("__shell__", Context.javaToJS(RhinoEngine.this, scope), hiddenConst)
            val predefJs = "predef.js"
            withCloseable(new InputStreamReader(getClass.getResourceAsStream(predefJs))) { reader =>
                cx.evaluateReader(scope, reader, predefJs, 1, null)
            }
            val enumerateProp = "enumerate"
            val enumerate = getProperty(enumerateProp)
            val enumerateableProps = enumerate.asInstanceOf[NativeArray].toList
            deleteProperty(enumerateProp)
            getPropertyIds.foreach{ prop =>scope.setAttributes(prop, if(enumerateableProps.contains(prop)) const else hiddenConst)}
            scope
        }

        def run(string: String): AnyRef = withContext  { cx =>
            getResult(cx.evaluateString(scope, string, "<stdin>", 1, null))
        }

        def run(reader: Reader, fileName: String): AnyRef = withContext { cx =>
            getResult( withCloseable(reader) { reader =>
                    cx.evaluateReader(scope, reader, fileName, 1, null)
                }
            )
        }

        private def getResult(runner: => AnyRef): AnyRef = runner match {
            case f: NativeFunction => "function"
            case x => Context.toString(x)
        }
    }
}
