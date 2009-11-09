package dash

import ARM._
import java.util.UUID
import java.io.FileReader
import Config._
import org.mozilla.javascript.{NativeArray, ScriptableObject, NativeJavaObject}

trait RhinoEngine extends ScriptEngine {
  this: ClientSession =>

    private val argStr = "arguments"
    private val __desc__ = "__desc__"

    private var engine = new Engine(out, stdinName)

    def tabCompletion(id: UUID, prefix: String, cursor: Int) = {
      val trimmed = prefix.trim
      val matches = trimmed.lastIndexOf(".") match {
        case -1 => engine.getPropertyIds.filter(_.startsWith(trimmed))
        case dotIdx => {
          val partial = prefix.substring(dotIdx + 1, prefix.length)
          val objStr = prefix.substring(0, dotIdx)
          val possibleCompletions = try {
            engine.rawRun(objStr) match {
              case j: NativeJavaObject => {
                val actual = j.unwrap
                val fields = actual.getClass.getFields.map(_.getName)
                val methods = actual.getClass.getMethods.map(_.getName + '(')
                List[String]() ++ fields ++ methods
              }
              case s: ScriptableObject => engine.getPropertyIds(s)
              case _ => Nil
            }
          } catch {
            case e => Nil
          }
          possibleCompletions.filter(_.startsWith(partial)).sort(_ < _).map(objStr + '.' + _)
        }
      }

      new TabCompletionList(id, matches)
    }

    def reset = engine = new Engine(out, stdinName)

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
    import org.mozilla.javascript.{Context, NativeFunction, NativeJavaObject}
    import java.io.{Reader, InputStreamReader}
    import org.mozilla.javascript.EcmaError
    import org.mozilla.javascript.JavaScriptException
    class Engine(out: RemoteWriter, stdinName: String) extends RhinoScopeWrapper {
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

        /**
         * Possibly unwrap the real java exception that caused the error..
         */
        private def processRhinoErrors[R](block: => R): R = {
          try {
            block
          } catch {
            case je: JavaScriptException => {
              je.getValue match {
                case v: NativeJavaObject => {
                  v.unwrap match {
                    case t: Throwable => {
                      throw new ScriptException(je.getMessage, t)
                    }
                  }
                }
              }
              throw je
            }
          }
        }

        def rawRun(string: String): AnyRef = withContext  { cx =>
          cx.evaluateString(scope, string, stdinName, 1, null)
        }

        def run(string: String): AnyRef = withContext  { cx =>
            processRhinoErrors(getResult(cx.evaluateString(scope, string, stdinName, 1, null)))
        }

        def run(reader: Reader, fileName: String): AnyRef = withContext { cx =>
            processRhinoErrors(getResult( withCloseable(reader) { reader =>
                    cx.evaluateReader(scope, reader, fileName, 1, null)
                }
            ))
        }

        private def getResult(runner: => AnyRef): AnyRef = runner match {
            case f: NativeFunction => "function"
            case x => Context.toString(x)
        }
    }
}
