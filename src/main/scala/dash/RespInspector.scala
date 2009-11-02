package dash

import org.apache.mina.filter.reqres.{ResponseInspector, ResponseType}
import java.util.UUID
import Config._

class RespInspector extends ResponseInspector {
    def getRequestId(resp: AnyRef): AnyRef = {
      if(logging) {
        println("inspecting response: " + resp)
      }
      resp match {
        case Resp(reqId) => reqId
        case _ => null
      }
    }

    def getResponseType(message: AnyRef) = ResponseType.WHOLE
}
