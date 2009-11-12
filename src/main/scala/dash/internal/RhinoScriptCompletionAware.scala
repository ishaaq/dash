package dash.internal

trait RhinoScriptCompletionAware extends ScriptCompletionAware {
  override protected def checkScriptComplete(script: String) = RhinoScopeWrapper.withContext { cx => cx.stringIsCompilableUnit(script) }
}
