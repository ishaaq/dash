package dash.internal

import sun.org.mozilla.javascript.internal.{Context, Scriptable, ScriptableObject, Function, Wrapper}
import java.security.{AccessController, PrivilegedExceptionAction}
import java.lang.reflect.{Proxy, InvocationHandler, Method}

/**
 * A replacement for the missing JavaAdapter Rhino functionality that Sun
 * removed when they packaged up Rhino for javax.scripting. Works by
 * instantiating Java proxies.
 */
class JavaAdapter extends ScriptableObject with Function {
    def getClassName() = "JavaAdapter"

    def call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array[AnyRef]): AnyRef = construct(cx, scope, args)

    def construct(cx: Context, scope: Scriptable, args: Array[AnyRef]): Scriptable  = {
        if (args.length == 2) {
            val clazz = args(0) match {
              case wrapper: Wrapper => wrapper.unwrap match {
                  case c: Class[_] => c
                  case _ => null
              }
              case clazz: Class[_] => clazz
              case _ => null
            }
            val interface = if(clazz.isInterface) clazz else null
            if (interface == null) {
                throw Context.reportRuntimeError("JavaAdapter: first arg should be interface Class")
            }

            val topLevel = ScriptableObject.getTopLevelScope(scope);
            return Context.toObject(proxify(args(1),  clazz, topLevel), topLevel);
        } else {
            throw Context.reportRuntimeError("JavaAdapter requires two arguments");
        }
    }

    private def proxify[P](obj: AnyRef, interface: Class[P], scope: Scriptable): P =
        Proxy.newProxyInstance(interface.getClassLoader,
                               Array(interface),
                               new Handler[P](obj, scope)).asInstanceOf[P]
}

class Handler[T](actual: AnyRef, scope: Scriptable) extends InvocationHandler {
   def invoke(proxy: AnyRef, method: Method, args: Array[AnyRef]): AnyRef = {
           AccessController.doPrivileged(new PrivilegedExceptionAction[AnyRef] {
               def run = invoke(method.getName, args)
           });
   }

   private def invoke(methodName: String, args: Array[AnyRef]): AnyRef = {
     RhinoScopeWrapper.withContext { ctx =>
         val local = actual match {
           case null => scope
           case _ => actual match {
             case s: Scriptable => s
             case _ => Context.toObject(actual, scope)
           }
         }
         ScriptableObject.getProperty(local, methodName) match {
           case f: Function => {
             val pScope: Scriptable = f.getParentScope match {
               case null => scope
               case pScope => pScope
             }
             val sArgs = if(args != null) args.map(Context.javaToJS(_, scope)) else Array[AnyRef]()
             f.call(ctx, pScope, local, sArgs)
           }
           case _ => throw new NoSuchMethodException("no method: " + methodName)
         }
     }
   }
}
