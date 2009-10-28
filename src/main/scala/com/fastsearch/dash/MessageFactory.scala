package com.fastsearch.dash

import java.util.UUID
import java.io.{BufferedReader, File, FileReader}
import java.util.{List => JList, ArrayList => AList, Collections}
import jline.{ConsoleReader, History, CandidateListCompletionHandler, Completor}
import jline.ANSIBuffer
import scala.collection.immutable.Queue
import scala.actors.Actor.{actor, link, exit}
import scala.actors.{Future, AbstractActor}
import scala.actors.remote.Node
import Constants._

trait MessageFactory {
    def get: Message
    val id = UUID.randomUUID
    def out: {def print(str: String)}
    def err: {def print(str: String)}
}

class InteractiveMessageFactory(server: AbstractActor) extends MessageFactory with Completor {
    private val console = new ConsoleReader
    val out = System.out
    val err = new Decorator(System.err, red)

    console.setHistory(new History(new File(System.getProperty("user.home"), ".dash_history")))
    console.addCompletor(this)
    console.setCompletionHandler(new CandidateListCompletionHandler)

    def get = {
      console.readLine(green("dash> ")) match {
        case null => Bye(id)
        case eval => new Eval(id, eval)
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

class Decorator(actual: {def print(str: String)}, decorator: String => String) {
    def print(str: String) = actual.print(decorator(str))
}

class ScriptedMessageFactory(script: String, args: Array[String]) extends MessageFactory {
    private var hasRun = false
    val out = System.out
    val err = System.err
    def get = hasRun match {
      case false =>
        hasRun = true
        new Run(id, script, args)
      case true => new Bye(id)
    }
}
