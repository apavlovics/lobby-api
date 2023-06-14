package lv.continuum.lobby.zio.layer

import lv.continuum.lobby.protocol.Protocol.PushOut
import lv.continuum.lobby.protocol.ProtocolFormat
import zio.*

trait SubscribersHolder {

  def add(subscriber: Subscriber): UIO[Unit]

  def remove(subscriber: Subscriber): UIO[Unit]

  def broadcast(pushOut: PushOut): UIO[Unit]

  def subscribers: UIO[Set[Subscriber]]
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

  override def broadcast(pushOut: PushOut): UIO[Unit] = for {
    subscribers <- subscribersRef.get
    _ <- ZIO.foreachPar(subscribers) { _.send(pushOut) }.catchAll { throwable =>
      ZIO.logWarningCause(s"Failed to broadcast $pushOut", Cause.fail(throwable))
    }
  } yield ()

  override def subscribers: UIO[Set[Subscriber]] = subscribersRef.get
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
