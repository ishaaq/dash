package dash

import org.apache.mina.filter.reqres.{ResponseInspector, ResponseType}
import Config._

class RespInspector extends ResponseInspector {
    def getRequestId(resp: AnyRef): AnyRef = {
      if(logging) {
        println("inspecting response: " + resp)
      }
      resp match {
        case resp: Resp => resp.reqId
        case _ => null
      }
    }

    def getResponseType(message: AnyRef) = ResponseType.WHOLE
}
