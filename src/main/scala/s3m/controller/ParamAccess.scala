package s3m.controller

import s3m.Controller

trait ParamAccess {
  this: Controller =>

  /** @return a single value */
  def param(key: String): String = {
    val values = allParams.get(key)
    if (values != null) values(0) else throw new MissingParam(key)
  }

  def paramo(key: String): Option[String] = {
    val values = allParams.get(key)
    if (values == null) None else Some(values(0))
  }

  /** @return a collection of values */
  def params(key: String): Array[String] = {
    val values = allParams.get(key)
    if (values != null) values else throw new MissingParam(key)
  }

  def paramso(key: String): Option[Array[String]] = {
    val values = allParams.get(key)
    if (values == null) None else Some(values)
  }
}
