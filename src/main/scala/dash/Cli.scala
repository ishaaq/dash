package dash

import java.io.File
import java.lang.reflect.Method
import java.net.{MalformedURLException, URL, URLClassLoader}
import java.util.{List => JList}
import java.security.{AccessController, PrivilegedActionException, PrivilegedExceptionAction}
import org.kohsuke.args4j.{Option => Opt, CmdLineParser, Argument, CmdLineException}
import org.kohsuke.args4j.spi.StringArrayOptionHandler

object Cli {
  private val systemLoader = ClassLoader.getSystemClassLoader().asInstanceOf[URLClassLoader]
  private val method = classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL])
  method.setAccessible(true)

    def main(args: Array[String]) {
        CmdLineParser.registerHandler(classOf[Array[String]], classOf[StringArrayOptionHandler])
        val parser = new CmdLineParser(Args)
        try {
            parser.parseArgument(args : _*)
        } catch {
          case e: CmdLineException => printUsageAndExit(parser, Some(e.getMessage))
        }
        if(Args.help) {
            printUsageAndExit(parser)
        }
        if(Args.file != None && Args.pid == None) {
            throw new IllegalArgumentException("Cannot run in script mode without specifying a process id")
        }
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
        new Attacher(Args.pid, Args.file, Args.args).attach
    }

    def printUsageAndExit(parser: CmdLineParser): Unit = printUsageAndExit(parser, None)

    def printUsageAndExit(parser: CmdLineParser, error: Option[String]): Unit = {
        val (status, out) = if(error == None) (0, System.out) else {
          System.err.println(error.get)
          (1, System.err)
        }

        out.println("Usage: java -jar dash.jar <options> <script-arguments>")
        out.println("Where the script-arguments are optinal arguments for the script file.")
        out.println("Options:")

        parser.printUsage(out)
        exit(status)
    }

}

object Args {
    var file: Option[File] = None
    var pid: Option[String] = None
    var args = Array[String]()
    var help = false

    @Opt{val name = "-f", val usage = "The script file to run" }
    def setFile(file: File) = this.file = Some(file)

    @Opt{val name = "-p", val metaVar = "pid", val usage = "The process-id of the Java app to attach to" }
    def setPid(pid: String) = this.pid = Some(pid)

    @Opt{val name = "-h", val usage = "Print this usage help"}
    def setHelp(help: Boolean) = this.help = true

    @Argument
    def setArguments(args: Array[String]) = this.args = args
}
