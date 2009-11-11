package dash.internal

import sun.org.mozilla.javascript.internal.{ScriptableObject, Context, Undefined, ImporterTopLevel}

trait RhinoScopeWrapper {
    protected val scope = withContext { cx => new ImporterTopLevel(cx) }

    def withContext[R] = RhinoScopeWrapper.withContext[R] _

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

object RhinoScopeWrapper {
    def withContext[R](block: Context => R): R = {
      val cx = Context.enter
      try {
          block(cx)
      } finally {
        Context.exit
      }
    }
}
