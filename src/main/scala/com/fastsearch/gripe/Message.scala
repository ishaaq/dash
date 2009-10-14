package com.fastsearch.gripe

sealed class Message

case object Ack extends Message

case class TabCompletionRequest(prefix: String) extends Message
case class TabCompletionList(list: List[String]) extends Message

case class Load(filePath: String) extends Message

case class Run(filePath: String) extends Message
case class Command(command: String) extends Message
case class Response(response: String) extends Message

case object Halt extends Message
case object HaltAck extends Message
