package com.fastsearch.dash

import java.io.File
import java.util.{List => JList, Collections}
import jline.{ConsoleReader, History, CandidateListCompletionHandler, Completor}
import Config._

trait MessageFactory {
    def get: Req
    def out: Printer
    def err: Printer
}

class InteractiveMessageFactory(server: ServerPeer) extends MessageFactory with Completor {
    private val console = new ConsoleReader
    val out = System.out
    val err = new Decorator(System.err, red)

    console.setHistory(new History(new File(System.getProperty("user.home"), ".dash_history")))
    console.addCompletor(this)
    console.setCompletionHandler(new CandidateListCompletionHandler)

    def get = {
      console.readLine(green("dash> ")) match {
        case null => Bye()
        case eval => new Eval(eval)
      }
    }

    override def complete(buffer: String, cursor: Int, candidateList: JList[_]): Int = {
        val list = candidateList.asInstanceOf[JList[String]]
        if(buffer.trim.length > 0) {
            server !? (new TabCompletionRequest(buffer)) match {
              case None => buffer.length
              case Some(x) => x match {
                case TabCompletionList(_, completionList) => {
                  completionList.foreach(list.add(_))
                  Collections.sort(list)
                  0
                }
                case x => {
                  err.println("Invalid response: " + x)
                  0
                }
              }
            }
        } else {
            buffer.length
        }
    }
}

class Decorator(actual: Printer, decorator: String => String) {
    def print(str: String) = actual.print(decorator(str))
    def println(str: String) = print(str + "\n")
}

class ScriptedMessageFactory(script: String, args: Array[String]) extends MessageFactory {
    private var hasRun = false
    val out = System.out
    val err = System.err
    def get = hasRun match {
      case false =>
        hasRun = true
        new Run(script, args)
      case true => Bye()
    }
}
