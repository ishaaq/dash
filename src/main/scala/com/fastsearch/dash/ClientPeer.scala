package com.fastsearch.dash

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import org.apache.mina.core.RuntimeIoException
import org.apache.mina.core.future.ConnectFuture
import org.apache.mina.transport.socket.nio.NioSocketConnector
import org.apache.mina.filter.codec.ProtocolCodecFilter
import org.apache.mina.transport.socket.nio.NioProcessor
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory

class ClientPeer(port: Int, receive: Req => Unit, close: => Unit) {
    private val executor = Executors.newCachedThreadPool(DaemonThreadFactory)
    private val connector = {
        val connector = new NioSocketConnector(executor, new NioProcessor(executor))
        connector.setConnectTimeoutMillis(Config.requestTimeout)
        val chain = connector.getFilterChain()
        chain.addLast("codec", new ProtocolCodecFilter(new ObjectSerializationCodecFactory()))
        if(Config.logging) {
            chain.addLast("logger", new org.apache.mina.filter.logging.LoggingFilter)
        }
        connector.setHandler(new ServerSessionHandler)
        connector
    }

    private val ioSession = {
        val future = connector.connect(new InetSocketAddress(Config.localHost, port))
        future.awaitUninterruptibly
        future.getSession
    }

    def !(message: Message): Unit = ioSession.write(message)

    import org.apache.mina.core.session.IoSession
    import org.apache.mina.core.service.{IoHandler, IoHandlerAdapter}
    class ServerSessionHandler extends IoHandlerAdapter {
        override def messageReceived(ioSession: IoSession, message: AnyRef) = {
            message match {
              case req: Req => receive(req)
              case x => println("Unexpected dash request: " + x)
            }
        }

        override def sessionClosed(ioSession: IoSession) = {
          close
          executor.shutdown
        }
    }
}
