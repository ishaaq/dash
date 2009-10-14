package com.fastsearch.gripe

trait MessageFactory {
    def get: Message
}

class InteractiveMessageFactory extends MessageFactory {
    def get = {
      Console.readLine match {
        case null => Halt
        case command => new Command(command)
      }
    }
}
