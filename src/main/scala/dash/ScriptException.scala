package dash

import java.io.{PrintWriter, PrintStream}

class ScriptException(val message: String, val cause: Throwable) extends RuntimeException(cause) {
    override val getMessage = message
    override def printStackTrace(out: PrintStream) = cause.printStackTrace(out)
    override def printStackTrace(pw: PrintWriter) = cause.printStackTrace(pw)
    override def printStackTrace = cause.printStackTrace
}
