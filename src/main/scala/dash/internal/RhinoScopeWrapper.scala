package dash.internal

import sun.org.mozilla.javascript.internal.{Context, NativeArray}
import scala.language.implicitConversions

object RhinoScopeWrapper {
    implicit def nativeArr2StringArr(jsArr: NativeArray): Array[String] = {
      val arr = new Array[String](jsArr.getLength.asInstanceOf[Int])
      jsArr.getIds.foreach { x =>
        val indx = x.asInstanceOf[Int]
        arr(indx) = jsArr.get(indx, null) match {
          case null => null
          case str: String => str
          case x => x.toString
        }
      }
      arr
    }

    def withContext[R](block: Context => R): R = {
      val cx = Context.enter
      try {
          block(cx)
      } finally {
        Context.exit
      }
    }
}
