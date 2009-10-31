package com.fastsearch.dash

import java.io.File
import java.util.UUID
import com.sun.tools.attach.VirtualMachine
import collection.jcl.Conversions.convertList
import java.net.ServerSocket

class Attacher(pid: Option[Int], file: Option[File], args: Array[String]) {
    val dashHome = System.getProperty(Config.dashHomeClientProperty)
    val id = UUID.randomUUID

    def attach = {
      val client = new Client(id, file, args)
      attachVm(pid) match {
        case Left(status) => exit(status)
        case Right(vm) => {
          vm.loadAgent(dashHome + File.separator + "dash.jar", client.port.toString + "," + dashHome +"," + id)
          vm.detach
        }
      }
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
