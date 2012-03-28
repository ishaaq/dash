package dash

import java.io.File
import java.lang.management.ManagementFactory
import java.net.URLDecoder
import com.sun.tools.attach.VirtualMachine
import scala.collection.JavaConversions.collectionAsScalaIterable
import Config._
import java.security.{AccessController, PrivilegedExceptionAction}
import java.net.{URL, URLClassLoader}
import com.sun.tools.attach.VirtualMachineDescriptor

/**
 * Selects and attaches to a running JVM.
 */
class Attacher private (pid: Option[String]) {
    val vm = getVM

    private def error(message: String) = System.err.println(message)

    private def getVM: Either[String, VirtualMachine] = {
      pid match {
        case Some(pid) => Right(VmUtil.attach(pid))
        case None => {
          val vms = VmUtil.getAttachableVms
          if(vms.size == 0) {
              Left("No running JVMs to attach to!")
          } else {
            vms.zipWithIndex.foreach {
              case (vm, idx) =>
                println("[%s] %s %s %s".format(idx + 1, vm.id, vm.mainClass, vm.vmArgs))
            }

            readLine("Choose a JVM number to attach to [1" + (if (vms.length == 1) "] : " else " - %s] : ".format(vms.length))) match {
              case null => Left("")
              case input => input.trim match {
                case "" => Left("")
                case input => {
                  try {
                    input.toInt match {
                      case invalidId if invalidId < 1 || invalidId > vms.length =>
                        Left("Invalid JVM id: " + invalidId)
                      case id => Right(VmUtil.attach(vms(id - 1).id))
                    }
                  } catch {
                    case ex: NumberFormatException =>
                      Left("Invalid JVM id: " + input)
                  }
                }
              }
            }
          }
        }
      }
    }

    def attach(client: Client, timeout: Long = 0, scriptPath: Option[String] = None): Option[String] = {
      try {
          vm match {
            case Right(vm) => {
              val dashJar = new File(URLDecoder.decode(getClass.getProtectionDomain.getCodeSource.getLocation.getPath, "UTF-8"))
              if (dashJar.exists()) {
                // scripts either lie under the Sys-prop-supplied 'dash.scripts' or under a 'scripts' subdir of the dash install dir:
                val scriptsDir = scriptPath.getOrElse(Option(System.getProperty("dash.scripts")).getOrElse(new File(dashJar.getParentFile(), "scripts").getAbsolutePath()))
                vm.loadAgent(dashJar.getAbsolutePath(), client.server.port.toString + "," + scriptsDir +"," + client.id + "," + client.name)
                vm.detach
                client.waitForConnection(timeout)
                return None
              } else {
                return Some("Unable to locate dash.jar to attach to vm, '" + dashJar.getAbsolutePath() + "' is not a valid location.");
              }
            }
            case error => return error.left.toOption
          }
      } catch {
        case ex => return Some(ex.getClass.getSimpleName + ": " + ex.getMessage)
      }
    }
}

object Attacher {
  def apply(pid: Option[String]) = new Attacher(pid)
}
