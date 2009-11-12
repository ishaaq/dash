package dash

import java.util.UUID

/**
 * Contract for scripting engine implementations.
 */
trait ScriptEngine {
  this: ClientSession =>

  def reset: Unit
  def close: Unit
  protected def eval(command: String): AnyRef
  protected def eval(script: String, args: Array[String]): AnyRef
  def tabCompletion(reqId: UUID, prefix: String, cursor: Int): TabCompletionList
  def describe(root: String): Either[String, List[String]]
}
