package s3m

import s3m.controller._
import s3m.view.Renderer

trait Controller extends Env with Logger with ParamAccess with Filter with Renderer {
  // private var completed = false would cause warning:
  // the initialization is no longer be executed before the superclass is called
  private var completed: Boolean = _
  { completed = false }

  def complete = synchronized {
    if (completed) {
      // Print the stack trace so that application developers know where to fix
      try {
        throw new Exception
      } catch {
        case e => logger.warn("Double render", e)
      }
    } else {
      completed = true
      ctx.complete
    }
  }
}
