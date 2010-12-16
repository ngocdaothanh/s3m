package s3m.controller

import java.net.URL
import java.net.URLClassLoader

import scala.collection.mutable.{HashSet => MHashSet}

import com.impetus.annovention.{Discoverer, ClasspathDiscoverer}
import com.impetus.annovention.listener.MethodAnnotationDiscoveryListener

import s3m.{Path, Paths}

class PathAnnotationDiscoveryListener extends MethodAnnotationDiscoveryListener {
  def discovered(clazz: String, method: String, annotation: String) {
    println("Discovered Method(" + clazz + "." + method + ") with Annotation(" + annotation + ")");
  }

  def supportedAnnotations = Array(classOf[Path].getName, classOf[Paths].getName)
}

object AnnotationScanner {
  def test {
    val discoverer = new ClasspathDiscoverer
    discoverer.addAnnotationListener(new PathAnnotationDiscoveryListener)
    discoverer.discover
  }
}
