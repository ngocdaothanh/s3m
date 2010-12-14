package s3m

import org.slf4j.LoggerFactory

/** By default, the logger name is inferred from the class name. */
trait Logger {
  val logger = {
    LoggerFactory.getLogger(getClass)
  }
}
