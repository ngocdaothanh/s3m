package s3m.exception

class MissingParam(val key: String) extends Throwable(key)
