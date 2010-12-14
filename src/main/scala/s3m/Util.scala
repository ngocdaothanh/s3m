package s3m

object Util {
  /** Wraps a single String by an Array. */
  def toValues(value: String): Array[String] = Array(value)
}
