package com.fastsearch.dash

import java.io.File
import jline.ANSIBuffer.ANSICodes._

object Constants {
  implicit val sessionFactory = JavaScriptClientSessionFactory
    //implicit val sessionFactory = GroovyClientSessionFactory
    //implicit val sessionFactory = JRubyClientSessionFactory

  val dashHomeClientProperty = "dash.home"
  val requestTimeout = 2000
  def scriptDir(dashHome: String) = new File(dashHome, "scripts")

  def red(str: String) = attrib(31) + str + attrib(0)
  def green(str: String) = attrib(32) + str + attrib(0)
}
