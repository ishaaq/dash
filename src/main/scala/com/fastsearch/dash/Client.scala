package com.fastsearch.dash

import java.io.File
import java.util.UUID
import Config._

class Client(id: UUID, file: Option[File], args: Array[String]) {
    lazy val server = new ServerPeer(start, out, err)
    lazy val port = server.port

    private lazy val messageFactory = file match {
                            case None => new InteractiveMessageFactory(server)
                            case Some(script) => new ScriptedMessageFactory(script.getAbsolutePath, args)
                          }
    private lazy val out: Printer = messageFactory.out
    private lazy val err: Printer = messageFactory.err

    private def start: Unit = {
        def print(outs: List[Output]): Unit = outs.foreach ( _ match {
          case StandardOut(str) => out.print(str)
          case StandardErr(str) => err.print(str)
        })

        val processResponse: PartialFunction[Resp, Unit] = {
          case Success(_, outs, response) =>
            print(outs)
            out.println(">> " + response)
          case Error(_, outs, response) =>
            print(outs)
            err.println("ERR! " + response)
          case x => err.println("unexpected response: " + x)
        }

        while(true) {
            messageFactory.get match {
                case Bye() => exit
                case req: Req => {
                  server !? req match {
                    case None => err.println("ERR! did not get a response")
                    case Some(resp) => processResponse(resp)
                  }
                }
            }
        }
    }
}
