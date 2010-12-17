package s3m.controller

import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader

import scala.collection.mutable.{ArrayBuffer, HashMap => MHashMap}

import com.impetus.annovention.{Discoverer, ClasspathDiscoverer}
import com.impetus.annovention.listener.MethodAnnotationDiscoveryListener

import s3m._

/** Scan all classes to collect routes. */
class RouteCollector extends MethodAnnotationDiscoveryListener {
  //                              controller         action    HTTP methods   paths
  private val map = new MHashMap[(Class[Controller], Method), (Array[String], Array[String])]

  def collect: Array[Routes.Route]  = {
    val discoverer = new ClasspathDiscoverer
    discoverer.addAnnotationListener(this)
    discoverer.discover

    val buffer = new ArrayBuffer[Routes.Route]
    for ((key, value) <- map) {
      val (httpMethods, paths) = value

      // paths is always non-empty, see "discovered" method below

      if (httpMethods.isEmpty) {
        for (p <- paths) buffer.append((None, p, key))
      } else {
        for (hm <- httpMethods; p <- paths) buffer.append((Some(hm), p, key))
      }
    }
    buffer.toArray
  }

  def supportedAnnotations = Array(
    classOf[GET].getName,
    classOf[POST].getName,
    classOf[PUT].getName,
    classOf[DELETE].getName,
    classOf[Path].getName,
    classOf[Paths].getName)

  def discovered(className: String, methodName: String, _annotationName: String) {
    val klass  = Class.forName(className).asInstanceOf[Class[Controller]]
    val method = klass.getMethod(methodName)
    val key    = (klass, method)

    if (map.contains(key)) return

    val pathPrefix = {
      val pathAnnotation = klass.getAnnotation(classOf[Path])
      if (pathAnnotation != null) pathAnnotation.value else ""
    }

    val annotations = method.getAnnotations
    val httpMethods = new ArrayBuffer[String]
    val paths       = new ArrayBuffer[String]
    for (annotation <- annotations) {
      if (annotation.isInstanceOf[Path]) {
        paths.append(pathPrefix + annotation.asInstanceOf[Path].value)
      } else if (annotation.isInstanceOf[Paths]) {
        val pathsAnnotation = annotation.asInstanceOf[Paths]
        for (pv <- pathsAnnotation.value) paths.append(pathPrefix + pv)
      } else if (annotation.isInstanceOf[GET]) {
        httpMethods.append("GET")
      } else if (annotation.isInstanceOf[POST]) {
        httpMethods.append("POST")
      } else if (annotation.isInstanceOf[PUT]) {
        httpMethods.append("PUT")
      } else if (annotation.isInstanceOf[DELETE]) {
        httpMethods.append("DELETE")
      }
    }

    if (!paths.isEmpty) map(key) = (httpMethods.toArray, paths.toArray)
  }
}
