package com.fastsearch.gripe

import groovy.lang.{Binding, GroovyShell}
import java.util.UUID

class ClientSession(id: UUID) {
    val binding = new Binding
    val shell = new GroovyShell(binding)
    def run(command: String): Message = {
      try {
          shell.evaluate(command) match {
            case null => new Response(null)
            case x => new Response(x.toString)
          }
      } catch {
        case e => new ErrorResponse(e.getMessage)
      }
    }

    def close = {
      // nothing to do really
    }
}
