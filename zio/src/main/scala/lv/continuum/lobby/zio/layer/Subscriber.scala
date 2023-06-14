package lv.continuum.lobby.zio.layer

import lv.continuum.lobby.protocol.Protocol.{Out, PushOut}
import lv.continuum.lobby.protocol.ProtocolFormat
import zio.Task
import zio.http.ChannelEvent.Read
import zio.http.WebSocketChannel
import zio.http.WebSocketFrame.Text

trait Subscriber {

  def send(pushOut: PushOut): Task[Unit]
}

case class WebSocketSubscriber(
  channel: WebSocketChannel,
) extends Subscriber
    with ProtocolFormat {

  override def send(pushOut: PushOut): Task[Unit] =
    channel.send(Read(Text(toJson[Out](pushOut))))
}
