package s3m.controller

class MissingParam(val key: String) extends Throwable(key)
