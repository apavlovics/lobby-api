package lv.continuum.lobby.zio.layer

import lv.continuum.lobby.protocol.Protocol.Out
import lv.continuum.lobby.protocol.ProtocolFormat
import zio.Task
import zio.http.ChannelEvent.Read
import zio.http.WebSocketChannel
import zio.http.WebSocketFrame.Text

trait Subscriber {

  def send(out: Out): Task[Unit]
}

case class WebSocketSubscriber(
  channel: WebSocketChannel,
) extends Subscriber
    with ProtocolFormat {

  def send(out: Out): Task[Unit] = channel.send(Read(Text(toJson(out))))
}
