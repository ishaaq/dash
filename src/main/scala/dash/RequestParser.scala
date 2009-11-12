package dash

import scala.util.parsing.combinator.RegexParsers

/**
 * Parses requests made in the interactive console.
 */
class RequestParser extends RegexParsers {

  def parseRequest(str: String): Either[ParseError, Req] = {
    str match {
      case null => Right(Quit)
      case str => str.trim match {
        case "" => Right(Noop)
        case str => {
          if(str.startsWith(":")) {
            val parseResult = parseAll(parseCommand, str)
            parseResult match {
              case Success(command, _) => Right(command)
              case Failure(msg, next) => Left(ParseError(parseResult.toString))
              case Error(msg, next) => Left(ParseError(parseResult.toString))
            }
          } else {
            // not a command - must be an eval:
            Right(Eval(str))
          }
        }
      }
    }
  }

  private def parseCommand: Parser[Command] = ":" ~> ( quit | help | reset | desc)

  private def quit: Parser[Command] = (caseins("exit") | caseins("quit")) ^^ { case _ => Quit }

  private def help: Parser[Command] = caseins("help") ~> opt(name) ^^ {
        case Some(commandName) => new Help(commandName)
        case None => new Help()
  }
  private def reset: Parser[Command] = caseins("reset") ^^ { case _ => Reset }

  private def desc: Parser[Command] = caseins("desc") ~> opt(name) ^^ {
        case Some(name) => new Desc(name)
        case None => new Desc()
  }

  // a name consists of a string with no white-space chars
  private val name = "\\S+".r

  /**
   * Returns a case-insensitive version of the inputted regex pattern.
   */
  private def caseins(pattern: String) = ("(?i)" + pattern).r
}

case class ParseError(message: String)
