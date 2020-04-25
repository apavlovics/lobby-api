package lv.continuum.evolution.akka

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Source
import akka.stream.typed.scaladsl.ActorSource
import akka.stream.{Materializer, OverflowStrategy}
import lv.continuum.evolution.protocol.Protocol.PushOut

/** An actor and a source for delivering push notifications to subscribed clients. */
object PushSource {

  def apply()(implicit
    materializer: Materializer,
  ): (ActorRef[PushOut], Source[PushOut, NotUsed]) =
    ActorSource
      .actorRef[PushOut](
        completionMatcher = Map.empty,
        failureMatcher = Map.empty,
        bufferSize = 100,
        overflowStrategy = OverflowStrategy.fail,
      )
      .preMaterialize()
}
