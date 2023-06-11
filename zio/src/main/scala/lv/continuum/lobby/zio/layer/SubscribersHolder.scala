package lv.continuum.lobby.zio.layer

import lv.continuum.lobby.protocol.Protocol.Out
import lv.continuum.lobby.protocol.ProtocolFormat
import zio.*
import zio.http.*
import zio.http.ChannelEvent.Read
import zio.http.WebSocketFrame.Text

trait SubscribersHolder {

  def add(channel: WebSocketChannel): UIO[Unit]

  def remove(channel: WebSocketChannel): UIO[Unit]

  def broadcast(out: Out): Task[Unit]
}

class SubscribersHolderLive private (
  channelsRef: Ref[Set[WebSocketChannel]],
) extends SubscribersHolder
    with ProtocolFormat {

  override def add(channel: WebSocketChannel): UIO[Unit] =
    channelsRef.update(_ + channel)

  override def remove(channel: WebSocketChannel): UIO[Unit] =
    channelsRef.update(_ - channel)

  override def broadcast(out: Out): Task[Unit] = for {
    channels <- channelsRef.get
    _        <- ZIO.foreachPar(channels) { _.send(Read(Text(toJson(out)))) }
  } yield ()
}

object SubscribersHolderLive {

  val layer: ULayer[SubscribersHolder] =
    ZLayer {
      for {
        _           <- ZIO.logDebug("Creating new subscribers holder")
        channelsRef <- Ref.make(Set.empty[WebSocketChannel])
      } yield SubscribersHolderLive(channelsRef)
    }
}
