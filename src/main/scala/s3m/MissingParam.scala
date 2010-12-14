package s3m

class MissingParam(val key: String) extends Throwable(key)
