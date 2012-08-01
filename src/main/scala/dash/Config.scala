package dash

import java.io.File
import jline.ANSIBuffer.ANSICodes.attrib
import java.net.InetAddress.getByName

/**
 * Central configuration values. Be careful not to rely on static variables from here on the Server -
 * if you do so, then ensure that those are constants set at compile time and never ever change because
 * if they do change then you run the risk of seeing non-deterministic behaviour when two or more dash
 * Clients attach to the same Server.
 */
object Config {
  //def clientSession(scriptsDir: String, out: RemoteWriter, stdinName: String) = new ClientSession(scriptsDir, out, stdinName) with JavaScriptEngine
  def clientSession(scriptsDir: String, out: RemoteWriter, stdinName: String) = new ClientSession(scriptsDir, out, stdinName) with internal.RhinoScriptEngine

  val logging = false
  val requestTimeout = 2000L

  val version = "v0.3.2"

  val localHost = getByName("127.0.0.1")

  def red(str: String) = Red.code + str + End.code
  def green(str: String) = Green.code + str + End.code
  def bold(str: String) = Bold.code + str + End.code

  type Printer = {
    def println(str: String)
  }
}
