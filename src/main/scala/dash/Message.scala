package dash

import java.util.UUID

sealed trait Message

import org.apache.mina.filter.reqres.{Request => MRequest}
class Request(req: Req) extends MRequest(req.id, req, Config.requestTimeout) {
  val id = getId.asInstanceOf[UUID]
}

sealed abstract case class Req() extends Message {
  val id = UUID.randomUUID
}

case class Bye() extends Req
case class TabCompletionRequest(prefix: String) extends Req
case class Run(filePath: String, args: Array[String]) extends Req
case class Eval(command: String) extends Req

@serializable
sealed abstract case class Resp(val reqId: UUID) extends Message
case class TabCompletionList(id: UUID, list: List[String]) extends Resp(id)
case class Success(id: UUID, out: List[Output], response: String) extends Resp(id)
case class Error(id: UUID, out: List[Output], response: String) extends Resp(id)

@serializable
sealed class Output(val string: String)

case class StandardOut(str: String) extends Output(str)
case class StandardErr(str: String) extends Output(str)
