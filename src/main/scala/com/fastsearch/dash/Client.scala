package com.fastsearch.dash

import java.io.File
import java.util.UUID
import Constants._

class Client(id: UUID, file: Option[File], args: Array[String]) {
    lazy val server = new ServerPeer(start)
    lazy val port = server.port

    private def start: Unit = {
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

        val processResponse: PartialFunction[Resp, Unit] = {
          case Success(_, outs, response) =>
            print(outs)
            out.print(">> " + response + '\n')
          case Error(_, outs, response) =>
            print(outs)
            err.print("ERR! " + response + '\n')
          case x => println("unexpected response: " + x)
        }

        while(true) {
            messageFactory.get match {
                case Bye() => {
                  val future = server !! Bye()
                  future.await(Constants.requestTimeout)
                  exit
                }
                case req: Req => {
                  server !? req match {
                    case None => err.print("ERR! did not get a response")
                    case Some(resp) => processResponse(resp)
                  }
                }
            }
        }
    }
}
