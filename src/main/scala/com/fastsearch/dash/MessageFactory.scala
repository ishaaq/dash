package com.fastsearch.dash

import java.io.File
import java.util.{List => JList, Collections}
import jline.{ConsoleReader, History, CandidateListCompletionHandler, Completor}
import scala.actors.AbstractActor
import scala.actors.remote.Node
import Constants._

trait MessageFactory {
    def get: Message
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
        case null => Bye
        case eval => new Eval(eval)
      }
    }

    override def complete(buffer: String, cursor: Int, candidateList: JList[_]): Int = {
        val list = candidateList.asInstanceOf[JList[String]]
        if(buffer.trim.length > 0) {
            server !? (Constants.requestTimeout, new TabCompletionRequest(buffer)) match {
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
        new Run(script, args)
      case true => Bye
    }
}
