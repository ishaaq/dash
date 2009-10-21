package com.fastsearch.dash

import scala.actors.{AbstractActor, Actor}
import scala.actors.Actor.{loop, actor}
import scala.actors.remote.RemoteActor
import scala.actors.OutputChannel
import scala.actors.remote.Node
import java.util.UUID

class Client(server: AbstractActor, messageFactory: MessageFactory) extends Actor {
    val id = messageFactory.id
    private val sysOut = System.out
    private val sysErr = System.err

    def act {
        link(server)
        server !! Syn(id)
        val processResponse: PartialFunction[Any, Unit] = {
              case Ack =>
                println("client Ack received")
              case Success(outs, response) =>
                print(outs)
                sysOut.println(">> " + response + "\n")
              case Error(outs, response) =>
                print(outs)
                sysOut.println("ERR! " + response)
        }
        loop {
              val message = messageFactory.get
              message match {
                case Bye(_) => {
                  // we'll wait a little while for the server to ack..
                  server.!?(Constants.requestTimeout, message)
                  //.. and exit
                  System.exit(0)
                }
                case _ => processResponse(server.!?(message))
              }
        }
    }

    private def print(outs: List[Output]): Unit = outs.foreach ( _ match {
      case StandardOut(str) => sysOut.print(str)
      case StandardErr(str) => sysErr.print(str)
    })
}
