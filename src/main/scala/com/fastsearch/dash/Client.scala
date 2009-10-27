package com.fastsearch.dash

import scala.actors.{AbstractActor, Actor}
import scala.actors.Actor.{loop, actor}
import scala.actors.remote.RemoteActor
import scala.actors.OutputChannel
import scala.actors.remote.Node
import java.util.UUID
import Constants._

class Client(server: AbstractActor, messageFactory: MessageFactory) extends Actor {
    val id = messageFactory.id
    private val out: {def print(str: String)} = messageFactory.out
    private val err: {def print(str: String)} = messageFactory.err

    def act {
        link(server)
        server !! Syn(id)
        val processResponse: PartialFunction[Any, Unit] = {
              case Ack =>
                println("client Ack received")
              case Success(outs, response) =>
                print(outs)
                out.print(">> " + response + '\n')
              case Error(outs, response) =>
                print(outs)
                err.print("ERR! " + response + '\n')
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
      case StandardOut(str) => out.print(str)
      case StandardErr(str) => err.print(str)
    })
}
