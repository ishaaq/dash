package com.fastsearch.grasp

import java.io.File
import com.sun.tools.attach._
import collection.jcl.Conversions._

class Attacher {
    def attach = {
      vmDescriptor match {
        case Left(status) => exit(status)
        case Right(vmDesc) => {
            val vm = VirtualMachine.attach(vmDesc.id)
            vm.loadAgent(System.getProperty("user.dir") + File.separator + "grasp-0.1.jar", "")
            vm.detach
        }
      }
    }

    private def vmDescriptor: Either[Int, VirtualMachineDescriptor] = {
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
            case id => Right(vms(id - 1))
          }
      } catch {
        case ex : NumberFormatException =>
          println("Invalid vm id!")
          Left(1)
      }
    }
}
