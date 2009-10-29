package com.fastsearch.dash

import java.io.File
import java.util.UUID
import com.sun.tools.attach.VirtualMachine
import collection.jcl.Conversions.convertList
import java.net.ServerSocket

class Attacher(pid: Option[Int], file: Option[File], args: Array[String]) {
    val dashHome = System.getProperty(Constants.dashHomeClientProperty)

    val id = Symbol(UUID.randomUUID.toString)
    def attach = {
      val port = getEphemeralPort
      val client = new Client(id, port, file, args)
      attachVm(pid) match {
        case Left(status) => exit(status)
        case Right(vm) => {
          client.start
          vm.loadAgent(dashHome + File.separator + "dash.jar", port.toString + "," + dashHome +"," + id.name)
          vm.detach
        }
      }
    }

    /**
     * Its a shame to have to do this this way...
     */
    private def getEphemeralPort() = {
      val tmpSocket = new ServerSocket(0)
      val port = tmpSocket.getLocalPort
      tmpSocket.close
      port
    }

    private def attachVm(pid: Option[Int]): Either[Int, VirtualMachine] = {
      pid match {
        case Some(pid) => Right(VirtualMachine.attach(pid.toString))
        case None =>
          val vms = VirtualMachine.list.toList
          vms.zipWithIndex.foreach { case (vm, idx) =>
              println("[%s] %s %s".format(idx + 1, vm.id, vm.displayName))
          }

          print("Choose a VM number to attach to [1" + (if (vms.length == 1) "] : " else " - %s] : ".format(vms.length)))

          try {
              readInt match {
                case invalidId if invalidId < 1 || invalidId > vms.length =>
                  println("Invalid vm id: %s".format(invalidId))
                  Left(1)
                case id => Right(VirtualMachine.attach(vms(id - 1).id))
              }
          } catch {
            case ex : NumberFormatException =>
              println("Invalid vm id!")
              Left(1)
          }
      }
    }
}
