package s3m.controller

import s3m.{Config, Logger, Util}

import java.lang.reflect.{Method, InvocationTargetException}
import java.util.{Map => JMap, List => JList, LinkedHashMap}

import javax.servlet.{AsyncContext, Filter => SFilter, FilterConfig, ServletRequest, ServletResponse, FilterChain}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

class Dispatcher extends SFilter with Logger {
  def init(filterConfig: FilterConfig) {
    Routes.collectAndCompile
  }

  def destroy {}

  def doFilter(
      servletRequest:  ServletRequest,
      servletResponse: ServletResponse,
      chain:           FilterChain) {

    val request  = servletRequest.asInstanceOf[HttpServletRequest]

    val method = overrideMethod(request)
    Routes.matchRoute(method, request.getPathInfo) match {
      case Some((ka, pathParams)) =>
        val response = servletResponse.asInstanceOf[HttpServletResponse]
        dispatchWithFailsafe(request, response, method, ka, pathParams)

      case None =>
        chain.doFilter(servletRequest, servletResponse)
    }
  }

  //----------------------------------------------------------------------------

  private def dispatchWithFailsafe(
      request:    HttpServletRequest,
      response:   HttpServletResponse,
      method:     String,
      ka:         Routes.KA,
      pathParams: Env.Params) {

    // For access log
    val beginTimestamp = System.currentTimeMillis

    val ctx             = request.startAsync(request, response)
    val (klass, action) = ka
    val controller      = klass.newInstance
    controller(ctx, method, pathParams)  // See Env

    try {
      // Call before filters
      val passed = controller.beforeFilters.forall(filter => {
        val onlyActions = filter._2
        if (onlyActions.isEmpty) {
          val exceptActions = filter._3
          if (!exceptActions.contains(action)) {
            val method = filter._1
            method.invoke(controller).asInstanceOf[Boolean]
          }	else true
        } else {
          if (onlyActions.contains(action)) {
            val method = filter._1
            method.invoke(controller).asInstanceOf[Boolean]
          } else true
        }
      })

      // Call action
      if (passed) action.invoke(controller)

      logAccess(controller, beginTimestamp)
    } catch {
      // MissingParam is a special case
      case e =>
        logAccess(controller, beginTimestamp)

        if (e.isInstanceOf[InvocationTargetException]) {
          val ite = e.asInstanceOf[InvocationTargetException]
          val c = ite.getCause
          if (c.isInstanceOf[MissingParam]) {
            response.sendError(400)  // Bad request
            val mp  = c.asInstanceOf[MissingParam]
            val key = mp.key
            val msg = "Missing Param: " + key
            response.getWriter.print(msg)
            ctx.complete
          } else {
            throw c
          }
        } else {
          throw e
        }
    }
  }

  //----------------------------------------------------------------------------

  def overrideMethod(request: HttpServletRequest): String = {
    val method = request.getMethod
    if (method == "POST") {
      val _method = request.getParameter("_method")
      if (_method != null) return _method
    }

    method
  }

  def logAccess(env: Env, beginTimestamp: Long) {
    val params   = filterParams(env.allParams)
    val endTimestamp = System.currentTimeMillis

    val msg = "%s %s %d [ms]".format(env.method, env.request.getPathInfo, params.toString, endTimestamp - beginTimestamp)
    logger.debug(msg)
  }

  // Same as Rails' config.filter_parameters
  private def filterParams(params: Env.Params): Env.Params = {
    val ret = new java.util.LinkedHashMap[String, Array[String]]()
    ret.putAll(params)
    for (key <- Config.filterParams) {
      if (ret.containsKey(key)) ret.put(key, Util.toValues("[filtered]"))
    }
    ret
  }
}
