package dash.internal

import dash.ScriptCompletionAware

trait RhinoScriptCompletionAware extends ScriptCompletionAware {
  override protected def checkScriptComplete(script: String) = RhinoScopeWrapper.withContext { cx => cx.stringIsCompilableUnit(script) }
}
