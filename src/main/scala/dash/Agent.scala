package dash

import java.lang.instrument.Instrumentation
import Config._
import java.util.UUID

object Agent {
  def agentmain(args: String, instrumentation: Instrumentation) {
      val arguments = args.split(",")
      val port = arguments(0).toInt
      val dashHome = arguments(1)
      val id = UUID.fromString(arguments(2))
      new Server(id, port, arguments(1))
      println("connected to dash client: " + id)
  }
}
