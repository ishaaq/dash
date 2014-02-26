package dash

import scala.collection.JavaConversions.collectionAsScalaIterable
import java.io.File
import java.lang.management.ManagementFactory
import java.net.{URL, URLClassLoader}
import java.security.{AccessController, PrivilegedExceptionAction}
import sun.jvmstat.monitor.{VmIdentifier, MonitoredVmUtil, HostIdentifier, MonitoredHost}
import com.sun.tools.attach.VirtualMachine

/**
 * Retrieve locally running attachable vms.
 */
object VmUtil {
  private lazy val systemLoader = ClassLoader.getSystemClassLoader().asInstanceOf[URLClassLoader]
  private lazy val method = classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL])

  checkToolsJar

  private def checkToolsJar = {
    try {
      Class.forName("com.sun.tools.attach.VirtualMachine")
    } catch {
      case ex: ClassNotFoundException =>
      // The current JVM does not have the tools.jar in its classpath, so attempt to load it up dynamically:
        val javaHome = new File(System.getProperty("java.home"))
        // depending on the distribution, the tools jar could be in one of two locations relative to java.home, so check both:
        val toolJarOption = List("../lib/tools.jar", "lib/tools.jar").map(new File(javaHome, _)).find(_.isFile())
        toolJarOption match {
          case Some(toolJar) => loadJar(toolJar)
          case None => throw new InstantiationException("Could not locate 'tools.jar'. Is this really an Oracle JDK VM?")
        }
    }
  }

  private def loadJar(jar: File) = {
    method.setAccessible(true)
    AccessController.doPrivileged(
      new PrivilegedExceptionAction[AnyRef]() {
        override def run(): Object = method.invoke(systemLoader, jar.toURI.toURL)
    })
  }

  // Gets current app's pid - sucks to have to do it this way - if only Sun would fix
  // http://bugs.sun.com/view_bug.do?bug_id=4244896 !!
  private val myPid = ManagementFactory.getRuntimeMXBean.getName.split('@')(0)
  def getAttachableVms: List[Vm] = {
    val host = MonitoredHost.getMonitoredHost("localhost")
    val jvms = host.activeVms().filter{_.toString != myPid}.map{ entry =>
      val pid = entry.toString
      try {
        val vm = host.getMonitoredVm(new VmIdentifier(pid))
        MonitoredVmUtil.isAttachable(vm) match {
          case true =>
            val mainClass = MonitoredVmUtil.mainClass(vm, false)
            val vmArgs = MonitoredVmUtil.jvmArgs(vm)
            val args = MonitoredVmUtil.mainArgs(vm)
            Some(Vm(pid, mainClass, vmArgs, args))
          case false => None
        }
      } catch {
        case _: Throwable => None
      }
    }
    jvms.flatten.toList
  }

  def attach(pid: String) = VirtualMachine.attach(pid)
}

case class Vm(val id: String, val mainClass: String, val vmArgs: String, val args: String)
