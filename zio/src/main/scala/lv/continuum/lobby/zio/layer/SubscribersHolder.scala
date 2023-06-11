package lv.continuum.lobby.zio.layer

import lv.continuum.lobby.protocol.Protocol.Out
import lv.continuum.lobby.protocol.ProtocolFormat
import zio.*

trait SubscribersHolder {

  def add(subscriber: Subscriber): UIO[Unit]

  def remove(subscriber: Subscriber): UIO[Unit]

  def broadcast(out: Out): Task[Unit]
}

class SubscribersHolderLive private (
  subscribersRef: Ref[Set[Subscriber]],
) extends SubscribersHolder
    with ProtocolFormat {

  override def add(subscriber: Subscriber): UIO[Unit] =
    subscribersRef.update(_ + subscriber)

  override def remove(subscriber: Subscriber): UIO[Unit] =
    subscribersRef.update(_ - subscriber)

  override def broadcast(out: Out): Task[Unit] = for {
    channels <- subscribersRef.get
    _        <- ZIO.foreachPar(channels) { _.push(out) }
  } yield ()
}

object SubscribersHolderLive {

  val layer: ULayer[SubscribersHolder] =
    ZLayer {
      for {
        _              <- ZIO.logDebug("Creating new subscribers holder")
        subscribersRef <- Ref.make(Set.empty[Subscriber])
      } yield SubscribersHolderLive(subscribersRef)
    }
}
