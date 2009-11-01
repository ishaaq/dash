package dash

import java.util.UUID

trait ScriptEngine {
  this: ClientSession =>

  def reset: Unit
  def close: Unit
  protected def eval(command: String): AnyRef
  protected def eval(script: String, args: Array[String]): AnyRef
  def tabCompletion(reqId: UUID, prefix: String): TabCompletionList
}
