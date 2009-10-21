package com.fastsearch.dash

import java.util.UUID
import jline.{ConsoleReader, History}
import java.io.{BufferedReader, File, FileReader}
import scala.collection.immutable.Queue

trait MessageFactory {
    def get: Message
    val id = UUID.randomUUID
}

class InteractiveMessageFactory extends MessageFactory {
    private val console = new ConsoleReader
    console.setHistory(new History(new File(System.getProperty("user.home"), ".dash_history")))

    def get = {
      console.readLine("dash> ") match {
        case null => Bye(id)
        case command => new Command(id, command)
      }
    }
}

class ScriptedMessageFactory(script: String, args: Array[String]) extends MessageFactory {
    private var hasRun = false
    def get = hasRun match {
      case false =>
        hasRun = true
        new Run(id, script, args)
      case true => new Bye(id)
    }
}
