package dash

import Config._
/**
 * Tab completers can get the matching completions when a user hits the 'Tab' key
 * on the interactive console. Tab completers are configured in sequence and the
 * first completer to return a non-empty array will be used to display the results
 * to the user.
 */
trait TabCompleter {
    val EMPTY_ARR = Array[String]()
    /**
     * Returns the possible completions for the given line buffer. If the completer
     * has no suggestions it should return an empty array (not null). To keep the cursor
     * position accurate the buffer is not trimmed however the buffer is guaranteed to
     * not be null or even empty after trimming.
     */
    def getCompletions(buffer: String, cursor: Int): Array[String]
}

object CommandTabCompleter extends TabCompleter {
    private val commands: Set[String] = Set() ++ List.flatten(Help.helpList.map(_.aliases)).map(":" + _ + " ")
    def getCompletions(buffer: String, cursor: Int) = commands.filter(_.startsWith(buffer)).toArray
}


class RemoteTabCompleter(server: ServerPeer) extends TabCompleter {
    import java.util.Collections
    def getCompletions(buffer: String, cursor: Int) = {
        server !? (new TabCompletionRequest(buffer, cursor)) match {
              case None => EMPTY_ARR
              case Some(x) => x match {
                case TabCompletionList(_, completionList) => completionList.toArray
                case x => {
                    println(red("Invalid response: ") + x)
                    EMPTY_ARR
                }
            }
        }
    }
}
