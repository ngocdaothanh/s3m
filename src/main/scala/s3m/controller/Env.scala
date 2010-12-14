package s3m.controller

import java.util.{LinkedHashMap => JLinkedHashMap, Map => JMap}
import javax.servlet.AsyncContext
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

object Env {
  /**
   * Design decision: Java Map is used instead of Scala Map because Netty produces
   * Java Map and we want to avoid costly conversion from Java Map to Scala Map.
   */
  type Params = JMap[String, Array[String]]
}

/**
 * All core state variables for a request are here. All other variables in Helper
 * and Controller can be inferred from these variables.
 */
class Env {
  var ctx:              AsyncContext        = _
  var request:          HttpServletRequest  = _
  var response:         HttpServletResponse = _

  var method:           String              = _
  var pathParams:       Env.Params          = _

  lazy val allParams:   Env.Params = {
    val ret = new JLinkedHashMap[String, Array[String]]()
    // The order is important because we want the later to overwrite the former
    ret.putAll(request.getParameterMap)
    ret.putAll(pathParams)
    ret
  }

  def apply(ctx: AsyncContext, method: String, pathParams: Env.Params) {
    this.ctx        = ctx
    this.request    = ctx.getRequest.asInstanceOf[HttpServletRequest]
    this.response   = ctx.getResponse.asInstanceOf[HttpServletResponse]

    this.method     = method
    this.pathParams = pathParams
  }
}
