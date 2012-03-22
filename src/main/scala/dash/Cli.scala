package dash

import java.io.File
import org.kohsuke.args4j.{Option => Opt, CmdLineParser, Argument, CmdLineException}
import org.kohsuke.args4j.spi.StringArrayOptionHandler

/**
 * Main entry point.
 */
object Cli {
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
        val client = Args.file match {
          case None => new InteractiveClient
          case Some(file) => new ScriptedClient(file, Args.args)
        }

        Attacher(Args.pid).attach(client, 1000).foreach { errorMessage =>
          Console.err.println(errorMessage)
          sys.exit(1)
        }
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
        sys.exit(status)
    }
}

object Args {
    var file: Option[File] = None
    var pid: Option[String] = None
    var args = Array[String]()
    var help = false

    @Opt(name = "-f", usage = "The script file to run")
    def setFile(file: File) = this.file = Some(file)

    @Opt(name = "-p", metaVar = "pid", usage = "The process-id of the Java app to attach to")
    def setPid(pid: String) = this.pid = Some(pid)

    @Opt(name = "-h", usage = "Print this usage help")
    def setHelp(help: Boolean) = this.help = true

    @Argument
    def setArguments(args: Array[String]) = this.args = args
}
