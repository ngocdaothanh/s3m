package s3m

import java.util.Properties

object Config {
  /**
   * Allow this to be accessed from other places, so that other configs can be
   * stored in xitrum.properties
   */
  lazy val properties = {
    val ret = new Properties
    val stream = getClass.getClassLoader.getResourceAsStream("s3m.properties")
    if (stream != null) ret.load(stream)
    ret
  }

  lazy val isProductionMode = (System.getProperty("s3m.mode") == "production")

  lazy val filterParams = properties.getProperty("filter_params", "password").split(", ")
}
