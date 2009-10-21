package com.fastsearch.dash

import java.util.{UUID, Map => JMap, Set => JSet}
import java.io.{File, FileReader, BufferedReader}
import java.security.{AccessController, PrivilegedActionException, PrivilegedExceptionAction}
import java.lang.reflect.Method
import groovy.lang.{Binding, GroovyShell, GroovyClassLoader}
import org.codehaus.groovy.runtime.MethodClosure

import scala.collection.jcl.Conversions.convertSet


class GroovyClientSession(val id: UUID, val out: RemoteWriter) extends ClientSession {
    private val binding = new Binding
    private val shell = init
    private val scriptClass = "dash_script"

    private val field = classOf[GroovyClassLoader].getDeclaredField("classCache")
    field.setAccessible(true)

    eval("load={script->dashSession.eval(script, new String[0])}")
    eval("bootstrap.groovy", null)

    private def init = {
        binding.setProperty("out", out)
        binding.setProperty("dashSession", this)
        new GroovyShell(classOf[GroovyClientSession].getClassLoader, binding)
    }

    protected def eval(command: String) = runScript(command, scriptClass)

    protected def eval(scriptPath: String, args: Array[String]) = {
        val maybeScriptFile = new File(Constants.scriptDir, scriptPath)
        val file = if(maybeScriptFile.exists && maybeScriptFile.isFile) maybeScriptFile else new File(scriptPath)
        val reader = new BufferedReader(new FileReader(file))
        val sb = new StringBuilder
        while(reader.ready) {
          sb.append(reader.readLine).append('\n')
        }
        try {
            binding.setProperty("args", args)
            runScript(sb.toString, scriptClass)
        } finally {
          binding.setProperty("args", null)
        }
    }

    def tabCompletion(prefix: String) = {
      val trimmed = prefix.trim
      val set: JSet[String] = binding.getVariables.keySet.asInstanceOf[JSet[String]]
      val matches = set.filter( _.startsWith(trimmed)).map( arg =>
                                binding.getVariable(arg) match {
                                    case m: MethodClosure =>
                                      m.getMaximumNumberOfParameters match {
                                        case 0 => arg + "()"
                                        case _ => arg + "("
                                      }
                                    case x => arg
                                  })
      new TabCompletionList(matches.toList)
    }

    private def runScript(scriptStr: String, name: String): AnyRef = {
      var clazz: Class[_] = null
      try {
        val script = shell.parse(scriptStr, name)
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


