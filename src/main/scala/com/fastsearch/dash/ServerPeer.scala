package com.fastsearch.dash

import Constants._
import java.net.InetSocketAddress
import org.apache.mina.filter.codec.ProtocolCodecFilter
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory
import org.apache.mina.transport.socket.nio.NioSocketAcceptor
import org.apache.mina.core.session.IoSession
import org.apache.mina.filter.reqres.RequestResponseFilter
import java.util.concurrent.ScheduledThreadPoolExecutor

class ServerPeer(start: => Unit) {
  private var session: IoSession = _
  private val executor = new ScheduledThreadPoolExecutor(5)
  lazy val acceptor = {
      val acc = new NioSocketAcceptor
      val chain = acc.getFilterChain
      chain.addLast("codec", new ProtocolCodecFilter(new ObjectSerializationCodecFactory()))
//      chain.addLast("logger", new org.apache.mina.filter.logging.LoggingFilter)
      chain.addLast("reqresp", new RequestResponseFilter(new RespInspector, executor))
      acc.setHandler(new ClientSessionHandler)
      acc.bind(new InetSocketAddress(localHost, 0))
      acc
  }
  lazy val port = acceptor.getLocalAddress.getPort

  def !!(req: Req) = session.write(new Request(req))

  def !(req: Req): Unit = !!(req)

  def !?(req: Req): Option[Resp] = {
    val request = new Request(req)
    executor.submit(new Runnable {
        def run: Unit = session.write(request)
    })
    val resp = request.awaitResponse
    resp.getMessage match {
      case resp: Resp => Some(resp)
      case x => { println("unexpected response: " + x); None }
    }
  }

  import org.apache.mina.core.service.{IoHandler, IoHandlerAdapter}
  class ClientSessionHandler extends IoHandlerAdapter {
    override def sessionOpened(ioSession: IoSession) = {
      if(session == null) {
          session = ioSession
          executor.execute(new Runnable {
            def run = start
          })
      }
      // TODO - perhaps close the server socket here - to stop
      // any other client sockets?
    }
  }
}
