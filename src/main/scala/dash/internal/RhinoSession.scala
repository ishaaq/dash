package dash.internal

import sun.org.mozilla.javascript.internal.{ScriptableObject, Context, Undefined, ImporterTopLevel, NativeArray, NativeFunction, NativeJavaObject, JavaScriptException, LazilyLoadedCtor}
import sun.org.mozilla.javascript.internal.ScriptableObject.{DONTENUM, PERMANENT, READONLY}
import java.io.{Reader, InputStreamReader}
import ARM._
import RhinoScopeWrapper._

/**
 * Sets up the Rhino session, creating and wrapping a top-level Rhino scope instance.
 */
class RhinoSession(engine: RhinoScriptEngine, out: RemoteWriter, stdinName: String) {
    protected val scope = withContext { cx => new ImporterTopLevel(cx) }

    withContext { cx =>
        new LazilyLoadedCtor(scope, "JSAdapter", "com.sun.script.javascript.JSAdapter", false)
        val hiddenConst = READONLY | PERMANENT | DONTENUM
        val const = READONLY | PERMANENT
        scope.defineProperty("out", Context.javaToJS(out, scope), const)
        scope.defineProperty("__shell__", Context.javaToJS(engine, scope), hiddenConst)
        val adapter = new JavaAdapter()
        adapter.setParentScope(scope)
        adapter.setPrototype(ScriptableObject.getFunctionPrototype(scope))
        scope.defineProperty("JavaAdapter", adapter, hiddenConst);

        val predefJs = "predef.js"
        withCloseable(new InputStreamReader(getClass.getResourceAsStream(predefJs))) { reader =>
            cx.evaluateReader(scope, reader, predefJs, 1, null)
        }
        val enumerateProp = "enumerate"
        val enumerate = getProperty(enumerateProp)
        val enumerateableProps = enumerate.asInstanceOf[NativeArray]
        deleteProperty(enumerateProp)
        (getPropertyIds ++ List("importClass", "importPackage")).foreach { prop =>
          scope.setAttributes(prop, if(enumerateableProps.contains(prop)) const else hiddenConst)
        }
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
        processRhinoErrors(getResult(withCloseable(reader) { reader =>
                cx.evaluateReader(scope, reader, fileName, 1, null)
            }
        ))
    }

    private def getResult(runner: => AnyRef): AnyRef = runner match {
        case f: NativeFunction => "function"
        case x => Context.toString(x)
    }

    def getPropertyIds(obj: ScriptableObject): List[String] = {
      val propIds = (for(propId <- ScriptableObject.getPropertyIds(obj)
          if(propId.isInstanceOf[String])
      ) yield propId.toString).toList
      propIds.sort((x, y) => x.toLowerCase < y.toLowerCase)
    }
    def getPropertyIds: List[String] = getPropertyIds(scope)

    def setProperty(obj: ScriptableObject)(prop: String, value: AnyRef): Unit = ScriptableObject.putProperty(obj, prop, value)
    def setProperty: (String, AnyRef) => Unit = setProperty(scope)_

    def hasProperty(obj: ScriptableObject)(prop: String) = ScriptableObject.hasProperty(obj, prop)
    def hasProperty: String => Boolean = hasProperty(scope)_

    def getProperty(obj: ScriptableObject)(prop: String) = ScriptableObject.getProperty(obj, prop)
    def getProperty: String => AnyRef = getProperty(scope)_

    def deleteProperty(obj: ScriptableObject)(prop: String) = ScriptableObject.deleteProperty(obj, prop)
    def deleteProperty: String => Boolean = deleteProperty(scope)_
}
