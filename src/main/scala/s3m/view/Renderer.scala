package s3m.view

import s3m.Controller

import java.io.File
import scala.xml.NodeSeq

trait Renderer extends {
  this: Controller =>

  //----------------------------------------------------------------------------

  def renderText(text: Any): String = {
    val string = text.toString
    response.getWriter.print(string)
    complete
    string
  }

  //----------------------------------------------------------------------------

  def renderBinary(bytes: Array[Byte]) {
    val os = response.getOutputStream
    os.write(bytes)
    os.flush
    complete
  }
}
