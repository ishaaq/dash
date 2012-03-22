package dash

import Config._
import java.net.InetSocketAddress
import org.apache.mina.filter.codec.ProtocolCodecFilter
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory
import org.apache.mina.transport.socket.nio.NioSocketAcceptor
import org.apache.mina.core.session.IoSession
import org.apache.mina.filter.reqres.RequestResponseFilter
import java.util.concurrent.ScheduledThreadPoolExecutor
import org.apache.mina.transport.socket.nio.NioProcessor
import org.apache.mina.core.service.SimpleIoProcessorPool

/**
 * The Client's peer connection to the Server.
 */
class ServerPeer(start: => Unit, client: Client) {
  private var session: IoSession = _
  private val executor = new ScheduledThreadPoolExecutor(5, DaemonThreadFactory)
  private lazy val acceptor = {
      val acc = new NioSocketAcceptor(executor, new SimpleIoProcessorPool(classOf[NioProcessor], executor))
      val chain = acc.getFilterChain
      chain.addLast("codec", new ProtocolCodecFilter(new ObjectSerializationCodecFactory()))
      if(logging) {
          chain.addLast("logger", new org.apache.mina.filter.logging.LoggingFilter)
      }
      chain.addLast("reqresp", new RequestResponseFilter(new RespInspector, executor))
      acc.setHandler(new ClientSessionHandler)
      acc.setCloseOnDeactivation(false) // for security reasons we will unbind the port as soon as one connection is made
      acc.bind(new InetSocketAddress(localHost, 0))
      acc
  }
  lazy val port = acceptor.getLocalAddress.getPort

  /**
   * Sends an asynchronous request. Returns a future.
   */
  def !!(req: Req) = session.write(req match {
        case req: ResponseRequired => new Request(req)
        case _ => req
      })

  /**
   * Sends a synchronous request, returns an option of a response.
   */
  def !?(req: ResponseRequired): Option[Resp] = {
    val request = new Request(req)
    executor.submit(new Runnable {
        def run: Unit = session.write(request)
    })
    val resp = request.awaitResponse
    resp.getMessage match {
      case resp: Resp => Some(resp)
      case x => { client.println(red("unexpected response: ") + x); None }
    }
  }

  @volatile var halted = false
  def shutdown: Unit = {
    if (!halted) {
      halted = true
      if (session.isConnected) session.close(true)
      executor.shutdownNow
    }
  }

  import org.apache.mina.core.service.IoHandlerAdapter
  class ClientSessionHandler extends IoHandlerAdapter {
    override def sessionOpened(ioSession: IoSession) = {
      acceptor.unbind() // we've established a connection, no need to continue exposing a security hole by leaving the server port bound
      if(session == null) {
          session = ioSession
          val thread = new Thread(new Runnable { def run = start}, "dashMainLoop") // we purposely don't use the executor here because we want a non-daemon thread for the main loop
          thread.setDaemon(false)
          thread.start()
      }
    }

    override def sessionClosed(session: IoSession): Unit = {
      if (!halted) {
        client.serverAbort
        shutdown
      }
    }

    override def messageReceived(session: IoSession, message: AnyRef): Unit = {
      message match {
        case Print(string) => client.print(string)
        case _ =>
      }
    }
  }
}
