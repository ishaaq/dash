package dash

object ARM {
  type Closeable = {def close(): Unit}

  def withCloseable[C<:Closeable, R](getCloseable: => C)(block: C => R): R = {
      val closeable = getCloseable
      try {
        block(closeable)
      } finally {
        closeable.close
      }
  }
}
