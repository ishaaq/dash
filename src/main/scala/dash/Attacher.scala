package dash

import java.io.File
import java.util.UUID
import com.sun.tools.attach.VirtualMachine
import collection.jcl.Conversions.convertList
import java.net.ServerSocket
import java.lang.management.ManagementFactory
import Config._

/**
 * Selects and attaches to a running JVM.
 */
class Attacher(pid: Option[String], file: Option[File], args: Array[String]) {
    val dashHome = System.getProperty(Config.dashHomeClientProperty)
    val id = UUID.randomUUID

    private def error(message: String) = System.err.println(message)

    def attach = {
      val client = new Client(id, file, args)
      try {
          attachVm(pid) match {
            case Left(errorMessage) => {
              if(errorMessage != null) error(errorMessage)
              exit(1)
            }
            case Right(vm) => {
              val stdinName = file match {
                case None => "<stdin>"
                case Some(file) => file.getName
              }
              vm.loadAgent(dashHome + File.separator + "dash.jar", client.port.toString + "," + dashHome +"," + id + "," + stdinName)
              vm.detach
            }
          }
      } catch {
        case ex => {
          error(ex.getClass.getSimpleName + ": " + ex.getMessage)
          exit(1)
        }
      }
    }

    private def attachVm(pid: Option[String]): Either[String, VirtualMachine] = {
      // Gets current app's pid - sucks to have to do it this way - if only Sun would fix
      // http://bugs.sun.com/view_bug.do?bug_id=4244896 !!
      val myPid = ManagementFactory.getRuntimeMXBean.getName.split('@')(0)
      // remove dash from the list of vms, no point attaching to oneself!
      val vms = VirtualMachine.list.toList.filter(_.id != myPid)
      pid match {
        case Some(pid) => {
          vms.find(_.id == pid) match {
            case None => Left("'%s' is not a Java process id!".format(pid))
            case Some(_) => Right(VirtualMachine.attach(pid))
          }
        }
        case None => {
          if(vms.size == 0) {
              Left("No running JVMs to attach to!")
          } else {
              vms.zipWithIndex.foreach { case (vm, idx) =>
                  println("[%s] %s %s".format(idx + 1, vm.id, vm.displayName))
              }

              print("Choose a JVM number to attach to [1" + (if (vms.length == 1) "] : " else " - %s] : ".format(vms.length)))

              readLine match {
                case null => Left("")
                case input => input.trim match {
                  case "" => Left(null)
                  case input => {
                    try {
                        input.toInt match {
                          case invalidId if invalidId < 1 || invalidId > vms.length =>
                            Left("Invalid JVM id: " + invalidId)
                          case id => Right(VirtualMachine.attach(vms(id - 1).id))
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
}
