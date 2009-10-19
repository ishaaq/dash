package com.fastsearch.dash

import java.util.{UUID, Map => JMap}
import java.io.{File, FileReader, BufferedReader}
import java.security.{AccessController, PrivilegedActionException, PrivilegedExceptionAction}
import java.lang.reflect.Method
import groovy.lang.{Binding, GroovyShell, GroovyClassLoader}
import org.codehaus.groovy.runtime.MethodClosure

class GroovyClientSession(val id: UUID, val out: RemoteWriter) extends ClientSession {
    private val binding = new Binding
    private val shell = init

    private val field = classOf[GroovyClassLoader].getDeclaredField("classCache")
    field.setAccessible(true)

    private def init = {
        binding.setProperty("out", out)
        new GroovyShell(classOf[GroovyClientSession].getClassLoader, binding)
    }

    protected def eval(command: String): AnyRef = {
      var clazz: Class[_] = null
      try {
        val script = shell.parse(command, "dash_script")
        clazz = script.getClass
        val result = if(clazz.getDeclaredMethods.exists(_.getName == "main")) {
            script.run()
        } else {
            null
        }
        // Keep only the methods that have been defined in the script
        clazz.getDeclaredMethods.foreach { method =>
              val name = method.getName
            if (!(name == "main" || name == "run" || name.startsWith("super$") || name.startsWith("class$") || name.startsWith("$"))) {
                binding.setProperty(name,  new MethodClosure(clazz.newInstance(), name))
            }
        }
        return result
      } finally {
        val cache = getClassLoaderCache(shell.getClassLoader)
        if(clazz != null) {
            cache.remove(clazz.getName)
        }
        cache.remove("$_run_closure")
      }
    }

    protected def eval(script: String, args: Array[String]) = {
        val reader = new BufferedReader(new FileReader(new File(script)))
        val sb = new StringBuilder
        while(reader.ready) {
          sb.append(reader.readLine).append('\n')
        }
        binding.setProperty("args", args)
        eval(sb.toString)
    }

    // nothing to do really
    def close = {}

    private def getClassLoaderCache(classLoader: GroovyClassLoader): JMap[_, _] = {
        AccessController.doPrivileged(
            new PrivilegedExceptionAction[JMap[_, _]]() {
                override def run: JMap[_, _] = {
                    field.get(classLoader).asInstanceOf[JMap[_, _]]
                }
            })

    }
}

object GroovyClientSessionFactory extends ClientSessionFactory {
    def apply(id: UUID, out: RemoteWriter) = new GroovyClientSession(id, out)
}


