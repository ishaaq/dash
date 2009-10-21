package com.fastsearch.dash

import java.io.File

object Constants {
    //implicit val sessionFactory = JavaScriptClientSessionFactory
    implicit val sessionFactory = GroovyClientSessionFactory
    //implicit val sessionFactory = JRubyClientSessionFactory

  val actorName = 'dash
  val portProperty = "com.fastsearch.dash.port"
  val dashHomeProperty = "dash.home"
  val requestTimeout = 500
  lazy val dashHome = System.getProperty(dashHomeProperty)
  lazy val scriptDir = new File(dashHome, "scripts")
}
