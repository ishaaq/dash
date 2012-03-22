package dash

import jline.ANSIBuffer.ANSICodes.attrib

/**
 * ANSI-code formatted strings.
 */
class FString(val rawString: String) {
    def formatted = getString(true)
    def unformatted = getString(false)
    def getString(useFormatting: Boolean): String = {
      val controlSeq = FormattedStringParser.parse(rawString).get
      val s = (new StringBuilder() /: controlSeq) { (s, x) =>
         x match {
           case Str(string) => s.append(string)
           case x: ANSIControl => if(useFormatting) s.append(x.ansi)
         }
         s
      }
      s.toString
    }

    override def toString = rawString
}

object FString {
  def apply(string: String) = new FString(string)
}

sealed trait FormattedStringPart
case class Str(string: String) extends FormattedStringPart

sealed abstract class ANSIControl(val code: String, val ansi: String) extends FormattedStringPart
case object Red extends ANSIControl("{{red:", attrib(31))
case object Green extends ANSIControl("{{green:", attrib(32))
case object Bold extends ANSIControl("{{bold:", attrib(1) + attrib(30))
case object End extends ANSIControl(":}}", attrib(0))
