package com.fastsearch.gripe

import java.io.File
import java.net.ServerSocket
import com.sun.tools.attach._
import collection.jcl.Conversions._

class Attacher {
    def attach = {
      vmDescriptor match {
        case Left(status) => exit(status)
        case Right(vmDesc) => {
            val vm = VirtualMachine.attach(vmDesc.id)
            val port = getEphemeralPort
            val server = new GripeServer(port, new InteractiveMessageFactory)
            server.start
            vm.loadAgent(System.getProperty("user.dir") + File.separator + "gripe.jar", port.toString)
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
