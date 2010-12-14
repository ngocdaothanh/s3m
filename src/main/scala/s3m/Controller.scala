package s3m

import s3m.controller._
import s3m.view.Renderer

trait Controller extends Env with Logger with ParamAccess with Filter with Renderer {
  private var responded: Boolean = _

  def complete = synchronized {
    if (responded) {
      // Print the stack trace so that application developers know where to fix
      try {
        throw new Exception
      } catch {
        case e => logger.warn("Double respond", e)
      }
    } else {
      responded = true
      ctx.complete
    }
  }
}
