package dash

import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.Position

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

  private def parseCommand: Parser[Command] = ":" ~> ( quit | commandHelp | appHelp )

  private def quit: Parser[Command] = (caseins("exit") | caseins("quit")) ^^ { case _ => Quit }
  private def commandHelp: Parser[Command] = caseins("help") ~> "^[\\w]+".r ^^ { case command: String => new Help(command) }
  private def appHelp: Parser[Command] = caseins("help") ^^ { case _ => new Help() }

  /**
   * Returns a case-insensitive version of the inputted regex pattern.
   */
  private def caseins(pattern: String) = ("(?i)" + pattern).r
}

case class ParseError(message: String)
