package dash.internal

import java.util.UUID
import java.io.FileReader
import RhinoScopeWrapper._
import sun.org.mozilla.javascript.internal.{NativeArray, ScriptableObject, NativeJavaObject}

/**
 * This RhinoScriptEngine unlike the JavaScriptEngine relies on internal classes in the sun JVM. We're doing this
 * so that we can take advantage of the more advanced features available in Rhino instead of the handicapped
 * functionality available through the javax.scripting API. We also are doing this instead of using a proper
 * dependency to Rhino-proper because we want to reduce the number of deps dash has at runtime - reducing the
 * chances of classloader hell when attaching to an app that already has a different Rhino version in its
 * System classloader.
 */
trait RhinoScriptEngine extends ScriptEngine {
  this: ClientSession =>

    private val argStr = "arguments"
    private val __desc__ = "__desc__"

    private var rhinoSession = new RhinoSession(this, out, stdinName)

    def tabCompletion(id: UUID, prefix: String, cursor: Int) = {
      val trimmed = prefix.trim
      val matches = trimmed.lastIndexOf(".") match {
        case -1 => rhinoSession.getPropertyIds.filter(_.startsWith(trimmed))
        case dotIdx => {
          val partial = prefix.substring(dotIdx + 1, prefix.length)
          val objStr = prefix.substring(0, dotIdx)
          val possibleCompletions = try {
            rhinoSession.rawRun(objStr) match {
              case j: NativeJavaObject => {
                val actual = j.unwrap
                val fields = actual.getClass.getFields.map(_.getName)
                val methods = actual.getClass.getMethods.map(_.getName + '(')
                List[String]() ++ fields ++ methods
              }
              case s: ScriptableObject => rhinoSession.getPropertyIds(s)
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

    def reset = rhinoSession = new RhinoSession(this, out, stdinName)

    def close = rhinoSession = null

    protected def eval(command: String) = rhinoSession.run(command)
    protected def eval(script: String, args: Array[String]): AnyRef = {
      val oldArgs = if(rhinoSession.hasProperty(argStr)) Some(rhinoSession.getProperty(argStr)) else None
      try {
          rhinoSession.setProperty(argStr, args)
          rhinoSession.run(new FileReader(resolveScriptFile(script)), script)
      } finally {
          oldArgs match {
            case None => rhinoSession.deleteProperty(argStr)
            case Some(oldArgs) => rhinoSession.setProperty(argStr, oldArgs)
          }
      }
    }

    def run(script: String, args: NativeArray): AnyRef = eval(script, args)

    def describe(ref: String) = {
      val descArgs = ref match {
        case "" => rhinoSession.getPropertyIds.mkString("','")
        case ref => ref
      }
      try {
          rhinoSession.run(__desc__ + "('" + descArgs + "')")
          None
      } catch {
        case e => Some("An error occurred: " + e.getMessage)
      }
    }
}
