package lv.continuum.evolution.exception

import akka.http.scaladsl.model.ws.{ Message, TextMessage, BinaryMessage }

case class MessageNotSupportedException(message: String, cause: Throwable = None.orNull)
  extends Exception(message, cause) {

  def this(message: Message) {
    this(message match {
      case _: TextMessage   => "Text message is not supported"
      case _: BinaryMessage => "Binary message is not supported"
    })
  }
}
