package com.fastsearch.dash

import java.io.File
import com.sun.tools.attach.VirtualMachine
import collection.jcl.Conversions.convertList
import scala.actors.remote.Node
import scala.actors.remote.RemoteActor.select
import java.net.ServerSocket

class Attacher(pid: Option[Int], file: Option[File], args: Array[String]) {
    def attach = {
      val port = attachVm(pid) match {
        case Left(status) => exit(status)
        case Right(vm) => {
            val props = vm.getSystemProperties
            if(props.containsKey(Constants.portProperty)) {
              props.getProperty(Constants.portProperty).toInt
            } else {
              val port = getEphemeralPort
              vm.loadAgent(Constants.dashHome + File.separator + "dash.jar", port.toString + "," + Constants.dashHome)
              vm.detach
              port
            }
        }
      }
      val server = select(Node("127.0.0.1", port), Constants.actorName)
      val client = new Client(server, file match {
                                    case None => new InteractiveMessageFactory(server)
                                    case Some(script) => new ScriptedMessageFactory(script.getAbsolutePath, args)
                                  })
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
