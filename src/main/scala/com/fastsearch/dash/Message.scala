package com.fastsearch.dash

import java.util.UUID
import scala.actors.Future

@serializable
abstract sealed class Message

case class Syn(id: UUID) extends Message
case object Ack extends Message
case class Bye(id: UUID) extends Message

case class TabCompletionRequest(id: UUID, prefix: String) extends Message
case class TabCompletionList(list: List[String]) extends Message

case class Run(id: UUID, filePath: String, args: Array[String]) extends Message
case class Command(id: UUID, command: String) extends Message

case class Success(out: List[Output], response: String) extends Message
case class Error(out: List[Output], response: String) extends Message

@serializable
sealed class Output(val string: String)

case class StandardOut(str: String) extends Output(str)
case class StandardErr(str: String) extends Output(str)
