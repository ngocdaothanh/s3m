package s3m.controller

import s3m.{Config, Logger}
import s3m.exception._

import java.lang.reflect.{Method, InvocationTargetException}
import java.util.{Map => JMap, List => JList, LinkedHashMap => JLinkedHashMap}

import javax.servlet.{AsyncContext, Filter => SFilter, FilterConfig, ServletRequest, ServletResponse, FilterChain}
import javax.servlet.annotation.WebFilter
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

@WebFilter(asyncSupported=true)
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

    val method          = overrideMethod(request)
    val encodedPathInfo = request.getRequestURI.substring(request.getContextPath.length)
    Routes.matchRoute(method, encodedPathInfo) match {
      case Some((ka, pathParams)) =>
        val response = servletResponse.asInstanceOf[HttpServletResponse]
        if (dispatchWithFailsafe(request, response, method, ka, pathParams)) {
          chain.doFilter(servletRequest, servletResponse)
        }

      case None =>
        chain.doFilter(servletRequest, servletResponse)
    }
  }

  //----------------------------------------------------------------------------

  /** @return true to pass control to the next filter/servlet */
  private def dispatchWithFailsafe(
      request:    HttpServletRequest,
      response:   HttpServletResponse,
      method:     String,
      ka:         Routes.KA,
      pathParams: Env.Params): Boolean = {

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
      false
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
            false
          } else if (c.isInstanceOf[Pass]) {
            true
          } else if (c.isInstanceOf[Halt]) {
            ctx.complete
            false
          }	else {
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
    val params       = filterParams(env.allParams)
    val endTimestamp = System.currentTimeMillis

    val msg = "%s %s %s %d [ms]".format(env.method, env.request.getRequestURI, params.toString, endTimestamp - beginTimestamp)
    logger.debug(msg)
  }

  // Same as Rails' config.filter_parameters
  private def filterParams(params: Env.Params): Env.Params = {
    val ret = new JLinkedHashMap[String, Array[String]]()
    ret.putAll(params)
    for (key <- Config.filterParams) {
      if (ret.containsKey(key)) ret.put(key, Util.toValues("[filtered]"))
    }
    ret
  }
}
