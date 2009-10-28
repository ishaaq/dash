package com.fastsearch.dash

@serializable
abstract sealed class Message

case object Ack extends Message
case object Bye extends Message

case class TabCompletionRequest(prefix: String) extends Message
case class TabCompletionList(list: List[String]) extends Message

case class Run(filePath: String, args: Array[String]) extends Message
case class Eval(command: String) extends Message

case class Success(out: List[Output], response: String) extends Message
case class Error(out: List[Output], response: String) extends Message

@serializable
sealed class Output(val string: String)

case class StandardOut(str: String) extends Output(str)
case class StandardErr(str: String) extends Output(str)
