package dash

import scala.util.parsing.combinator.RegexParsers
object FormattedStringParser extends RegexParsers {

    override def skipWhitespace = false

    private val formats = List(end, red, green, bold)

    def parse(string: String) = parseAll(formattedString, string)

    private def formattedString: Parser[List[FormattedStringPart]] = rep(controls) <~ emptyString


    private def controls: Parser[FormattedStringPart] = (red | green | bold | end | string)

    private def red = Red.code ^^ { case _ => Red }
    private def green = Green.code ^^ { case _ => Green }
    private def bold = Bold.code ^^ { case _ => Bold }
    private def end = End.code ^^ { case _ => End }

    private def emptyString = ""

    private val formatCodes = List(Red.code, Green.code, Bold.code, End.code)
    private def string = new Parser[Str] {
        def apply(in: Input): ParseResult[Str] = {
            val source = in.source
            val offset = in.offset
            if(source.length == offset) {
                return  Failure("Unexpected EOF", in)
            }

            val substring = source.subSequence(offset, source.length).toString

            if(substring.length == 0) {
                return  Failure("Expected non-empty string", in)
            }
            val indxs = formatCodes.map(fmt => substring.indexOf(fmt)).filter(_ != -1).sort((i,j) => i < j)
            val matchString = indxs match {
              case Nil => substring
              case x::xs => substring.substring(0, x)
            }
            matchString.length match {
              case 0 =>
                    return  Failure("Expected EOF", in)
              case x =>
                  return  Success(Str(matchString), in.drop(x))
            }
        }
    }

    /**
     * just for testing - should really move this to scalacheck or some such...
     */
    def main(args: Array[String]): Unit = {
            val testStrings = List(
              new FormattedString("asdf"),
              new FormattedString("a{{red:b:}}c"),
              new FormattedString("asd{{red:qwe@:}}zxc"),
              new FormattedString("a{{red:b:}}{{bold:c@:}}dc"),
              new FormattedString("a{{red::}}{{bold:c@:}}dc")
            )
            testStrings.foreach { string =>
              println("unformatted: " + string.getString(false))
              println("formatted: " + string.getString(true))
            }
    }
}
