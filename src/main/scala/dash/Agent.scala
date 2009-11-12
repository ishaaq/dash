package dash

import java.lang.instrument.Instrumentation
import java.util.UUID

/**
 * An Attach API agent.
 */
object Agent {
  def agentmain(args: String, instrumentation: Instrumentation) {
      val arguments = args.split(",")
      val port = arguments(0).toInt
      val dashHome = arguments(1)
      val id = UUID.fromString(arguments(2))
      val stdinName = arguments(3)
      new Server(id, port, dashHome, stdinName)
      println("connected to dash client: " + id)
  }
}
