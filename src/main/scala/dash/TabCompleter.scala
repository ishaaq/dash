package dash

import Config._
/**
 * Tab completers can get the matching completions when a user hits the 'Tab' key
 * on the interactive console. Tab completers are configured in sequence and the
 * first completer to return a non-empty array will be used to display the results
 * to the user.
 */
trait TabCompleter {
    val NO_RESULT = (Array[String](), 0)
    /**
     * Returns the possible completions for the given line buffer. If the completer
     * has no suggestions it should return an empty array (not null). To keep the cursor
     * position accurate the buffer is not trimmed however the buffer is guaranteed to
     * not be null or even empty after trimming.
     */
    def getCompletions(buffer: String, cursor: Int): (Array[String], Int)
}

object CommandTabCompleter extends TabCompleter {
    private val commands: Set[String] = Set() ++ Help.helpList.map(_.aliases).flatten.map(":" + _ + " ")
    def getCompletions(buffer: String, cursor: Int) = (commands.filter(_.startsWith(buffer)).toArray, 0)
}

class DescTabCompleter(remoteTabCompleter: RemoteTabCompleter) extends TabCompleter {
  private val DescRegex = ("^(:" + new Desc().aliases(0) + "\\s)?(.*)").r
  def getCompletions(buffer: String, cursor: Int) = {
    val DescRegex(prefix, suffix) = buffer
    if(prefix != null) {
        val offset = prefix.length;
        val substring = buffer.substring(offset, buffer.length)
        substring.trim match {
          case "" => NO_RESULT
          case _ => remoteTabCompleter.getCompletions(substring, cursor) match {
            case NO_RESULT => NO_RESULT
            case (matches, matchOffset) => (matches, offset + matchOffset)
          }
        }
    } else {
        NO_RESULT
    }
  }
}

class RemoteTabCompleter(server: ServerPeer) extends TabCompleter {
    import java.util.Collections
    def getCompletions(buffer: String, cursor: Int) = {
        server !? (new TabCompletionRequest(buffer, cursor)) match {
              case None => NO_RESULT
              case Some(x) => x match {
                case TabCompletionList(_, completionList) => (completionList.toArray, 0)
                case x => {
                    println(red("Invalid response: ") + x)
                    NO_RESULT
                }
            }
        }
    }
}
