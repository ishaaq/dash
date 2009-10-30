package com.fastsearch.dash

import org.apache.mina.filter.reqres.{ResponseInspector, ResponseType}
import java.util.UUID

class RespInspector extends ResponseInspector {
    def getRequestId(resp: AnyRef): AnyRef = {
      resp match {
        case Resp(reqId) => reqId
        case _ => null
      }
    }

    def getResponseType(message: AnyRef) = ResponseType.WHOLE
}
