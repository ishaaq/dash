package com.fastsearch.dash

import java.util.UUID
import java.io.{BufferedReader, File, FileReader}
import java.util.{List => JList, ArrayList => AList, Collections}
import jline.{ConsoleReader, History, CandidateListCompletionHandler, Completor}
import scala.collection.immutable.Queue
import scala.actors.Actor.{actor, link, exit}
import scala.actors.{Future, AbstractActor}
import scala.actors.remote.Node

trait MessageFactory {
    def get: Message
    val id = UUID.randomUUID
}

class InteractiveMessageFactory(server: AbstractActor) extends MessageFactory with Completor {
    private val console = new ConsoleReader
    console.setHistory(new History(new File(System.getProperty("user.home"), ".dash_history")))
    console.addCompletor(this)
    console.setCompletionHandler(new CandidateListCompletionHandler)

    def get = {
      console.readLine("dash> ") match {
        case null => Bye(id)
        case command => new Command(id, command)
      }
    }

    override def complete(buffer: String, cursor: Int, candidateList: JList[_]): Int = {
        val list = candidateList.asInstanceOf[JList[String]]
        if(buffer.trim.length > 0) {
            server !? (Constants.requestTimeout, new TabCompletionRequest(id, buffer)) match {
              case None => buffer.length
              case Some(x) => x match {
                case TabCompletionList(completionList) =>
                  completionList.foreach(list.add(_))
                  Collections.sort(list)
                  0
              }
            }
        } else {
            buffer.length
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
