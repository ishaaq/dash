package com.fastsearch.gripe

import java.util.UUID

trait MessageFactory {
    def get: Message
    def id: UUID
}

class InteractiveMessageFactory extends MessageFactory {
    val id = UUID.randomUUID

    def get = {
      print("gripe> ")
      Console.readLine match {
        case null => Bye(id)
        case command => new Command(id, command)
      }
    }
}
