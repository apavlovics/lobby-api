package lv.continuum.lobby.zio.layer

import lv.continuum.lobby.protocol.Protocol.Out
import lv.continuum.lobby.protocol.ProtocolFormat
import zio.Task
import zio.http.ChannelEvent.Read
import zio.http.WebSocketChannel
import zio.http.WebSocketFrame.Text

class Subscriber(channel: WebSocketChannel) extends ProtocolFormat {

  def send(out: Out): Task[Unit] = channel.send(Read(Text(toJson(out))))
}
