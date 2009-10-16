package com.fastsearch.dash

import java.io.File
import com.sun.tools.attach._
import collection.jcl.Conversions._
import scala.actors.remote.Node
import java.net.ServerSocket


class Attacher {
    def attach = {
      val port = vmDescriptor match {
        case Left(status) => exit(status)
        case Right(vmDesc) => {
            val vm = VirtualMachine.attach(vmDesc.id)
            val props = vm.getSystemProperties
            if(props.containsKey(Server.portProperty)) {
              props.getProperty(Server.portProperty).toInt
            } else {
              val port = getEphemeralPort
              vm.loadAgent(System.getProperty("user.dir") + File.separator + "dash.jar", port.toString)
              vm.detach
              port
            }
        }
      }
      val node = Node("127.0.0.1", port)
      val client = new Client(node, new InteractiveMessageFactory)
      client.start
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
