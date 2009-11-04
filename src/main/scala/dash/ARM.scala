package dash

import java.io.Closeable

object ARM {
  /**
   * Structural Type closeable
   */
  type STypeCloseable = {def close(): Unit}

  implicit def sTypeCloseableToCloseable(stCloseable: STypeCloseable) = {
    new Closeable {
      def close = stCloseable.close
    }
  }

  def withCloseable[C<:Closeable, R](getCloseable: => C)(block: C => R): R = {
      val closeable = getCloseable
      try {
        block(closeable)
      } finally {
        closeable.close
      }
  }
}
