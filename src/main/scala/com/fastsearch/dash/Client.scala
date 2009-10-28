package com.fastsearch.dash

import scala.actors.Actor
import scala.actors.Actor.{loop, self}
import scala.actors.remote.RemoteActor.{alive, register}

import java.io.File
import Constants._

class Client(port: Int, file: Option[File], args: Array[String]) extends Actor {
    trapExit = true

    def act {
        alive(port)
        register(actorName, self)

        loop {
            receive {
              case Ack => {
                val server = sender.receiver
                link(server)
                val messageFactory = file match {
                                    case None => new InteractiveMessageFactory(server)
                                    case Some(script) => new ScriptedMessageFactory(script.getAbsolutePath, args)
                                  }
                val out: {def print(str: String)} = messageFactory.out
                val err: {def print(str: String)} = messageFactory.err
                def print(outs: List[Output]): Unit = outs.foreach ( _ match {
                  case StandardOut(str) => out.print(str)
                  case StandardErr(str) => err.print(str)
                })
                val processResponse: PartialFunction[Any, Unit] = {
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
                        case Bye => {
                          // we'll wait a little while for the server to ack..
                          server.!?(Constants.requestTimeout, message)
                          //.. and exit
                          System.exit(0)
                        }
                        case _ => processResponse(server.!?(message))
                    }
                }
              }
            }
         }
    }
}
