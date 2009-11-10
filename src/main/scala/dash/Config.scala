package dash

import java.io.File
import jline.ANSIBuffer.ANSICodes.attrib
import java.net.InetAddress.getByName

object Config {
  //def clientSession(dashHome: String, out: RemoteWriter, stdinName: String) = new ClientSession(dashHome, out, stdinName) with JavaScriptEngine
  def clientSession(dashHome: String, out: RemoteWriter, stdinName: String) = new ClientSession(dashHome, out, stdinName) with internal.RhinoEngine

  def messageFactory(option: Option[File], args: Array[String], server: ServerPeer): MessageFactory = {
    option match {
      case None => new InteractiveMessageFactoryImpl(server) with internal.RhinoScriptCompletionAware
      case Some(script) => new ScriptMessageFactory(script.getAbsolutePath, args)
    }
  }

  val logging = false
  val dashHomeClientProperty = "dash.home"
  val requestTimeout = 2000L
  def scriptDir(dashHome: String) = new File(dashHome, "scripts")

  val version = "v0.01"

  val localHost = getByName("127.0.0.1")

  def red(str: String) = Red.code + str + End.code
  def green(str: String) = Green.code + str + End.code
  def bold(str: String) = Bold.code + str + End.code

  type Printer = {
    def print(str: String)
    def println(str: String)
  }
}
