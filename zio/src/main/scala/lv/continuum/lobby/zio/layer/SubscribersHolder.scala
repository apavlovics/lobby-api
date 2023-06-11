package lv.continuum.lobby.zio.layer

import lv.continuum.lobby.protocol.Protocol.Out
import lv.continuum.lobby.protocol.ProtocolFormat
import zio.*

trait SubscribersHolder {

  def add(subscriber: Subscriber): UIO[Unit]

  def remove(subscriber: Subscriber): UIO[Unit]

  def broadcast(out: Out): UIO[Unit]
}

class SubscribersHolderLive private (
  subscribersRef: Ref[Set[Subscriber]],
) extends SubscribersHolder
    with ProtocolFormat {

  override def add(subscriber: Subscriber): UIO[Unit] = for {
    subscribers <- subscribersRef.updateAndGet(_ + subscriber)
    _           <- ZIO.logDebug(s"Added a subscriber, now there are ${subscribers.size}")
  } yield ()

  override def remove(subscriber: Subscriber): UIO[Unit] = for {
    subscribers <- subscribersRef.updateAndGet(_ - subscriber)
    _           <- ZIO.logDebug(s"Removed a subscriber, now there are ${subscribers.size}")
  } yield ()

  override def broadcast(out: Out): UIO[Unit] = for {
    subscribers <- subscribersRef.get
    _ <- ZIO.foreachPar(subscribers) { _.send(out) }.catchAll { throwable =>
      ZIO.logWarningCause(s"Failed to broadcast $out", Cause.fail(throwable))
    }
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
