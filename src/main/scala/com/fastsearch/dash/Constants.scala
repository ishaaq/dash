package com.fastsearch.dash

import java.io.File
import jline.ANSIBuffer.ANSICodes.attrib
import java.net.InetAddress.getByName

object Constants {
  implicit val sessionFactory = JavaScriptClientSessionFactory

  val dashHomeClientProperty = "dash.home"
  val requestTimeout = 2000
  def scriptDir(dashHome: String) = new File(dashHome, "scripts")

  val localHost = getByName("127.0.0.1")

  def red(str: String) = attrib(31) + str + attrib(0)
  def green(str: String) = attrib(32) + str + attrib(0)
}
