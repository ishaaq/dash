package com.fastsearch.grasp

import java.io.File
import java.lang.reflect.Method
import java.net.{MalformedURLException, URL, URLClassLoader}
import java.security.{AccessController, PrivilegedActionException, PrivilegedExceptionAction}

object GraspCLI {
  val systemLoader = ClassLoader.getSystemClassLoader().asInstanceOf[URLClassLoader];
  val method = classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL]);
  method.setAccessible(true)

  def main(args: Array[String]) {
    AccessController.doPrivileged(
      new PrivilegedExceptionAction[AnyRef]() {
          override def run(): Object = {
              val javaHome = new File(System.getProperty("java.home"))
              val toolJar = new File(javaHome, "lib/tools.jar").toURI.toURL
              val toolJarInJvmHome = new File(javaHome, "../lib/tools.jar").toURI.toURL
              method.invoke(systemLoader, toolJar)
              method.invoke(systemLoader, toolJarInJvmHome)
          }
      }
    )

    val attacher = new Attacher
    attacher.attach
  }
}
