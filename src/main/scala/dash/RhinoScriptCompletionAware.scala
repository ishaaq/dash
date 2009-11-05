package dash

trait RhinoScriptCompletionAware extends RhinoScopeWrapper with ScriptCompletionAware {
  override protected def checkScriptComplete(script: String) = {
      withContext { cx => cx.stringIsCompilableUnit(script)}
  }
}
