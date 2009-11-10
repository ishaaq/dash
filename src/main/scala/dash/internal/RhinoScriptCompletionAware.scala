package dash.internal

import Config._
trait RhinoScriptCompletionAware extends RhinoScopeWrapper with ScriptCompletionAware {
  override protected def checkScriptComplete(script: String) = {
      withContext { cx => cx.stringIsCompilableUnit(script)}
  }
}
