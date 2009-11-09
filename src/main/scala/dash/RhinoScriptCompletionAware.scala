package dash

import Config._
trait RhinoScriptCompletionAware extends RhinoScopeWrapper with ScriptCompletionAware with InteractiveMessageFactory {
  override protected def checkScriptComplete(script: String) = {
      withContext { cx => cx.stringIsCompilableUnit(script)}
  }

  override def welcomeMessage =
    green("dash (" + version + ")") +
        ": the {{bold:D:}}ynamically {{bold:A:}}ttaching {{bold:SH:}}ell\n" +
        "(using: " + getRhinoVersion + ")\n" +
"""==============================================
For help type {{bold::help:}} at the prompt."""

  def getRhinoVersion = withContext { cx => cx.getImplementationVersion }

}
