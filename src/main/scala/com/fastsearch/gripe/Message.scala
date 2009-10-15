package com.fastsearch.gripe

import java.util.UUID

sealed class Message

case class Syn(id: UUID) extends Message
case object Ack extends Message
case class Bye(id: UUID) extends Message

case class TabCompletionRequest(id: UUID, prefix: String) extends Message
case class TabCompletionList(list: List[String]) extends Message

case class Run(id: UUID, filePath: String, args: Array[String]) extends Message
case class Command(id: UUID, command: String) extends Message
case class Response(response: String) extends Message
case class ErrorResponse(response: String) extends Message

