package lv.continuum.evolution.akka

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.typed.scaladsl.{ActorFlow, ActorSource}
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import io.circe.Error
import io.circe.parser._
import io.circe.syntax._
import lv.continuum.evolution.akka.SessionActor.SessionCommand
import lv.continuum.evolution.protocol.Protocol._
import lv.continuum.evolution.protocol._

import scala.concurrent.duration._

object FlowCreator
  extends ProtocolFormat
    with LazyLogging {

  private val parallelism = Runtime.getRuntime.availableProcessors() * 2 - 1
  logger.info(s"Parallelism is $parallelism")

  private implicit val timeout: Timeout = Timeout(5.seconds)
  logger.info(s"Timeout is ${ timeout.duration }")

  /** Creates a source for delivering push notifications to subscribed clients. */
  def createPushSource(implicit
    materializer: Materializer,
  ): (ActorRef[PushOut], Source[PushOut, NotUsed]) = {
    // TODO Think about matchers and overflow strategy
    ActorSource.actorRef[PushOut](
      completionMatcher = Map.empty,
      failureMatcher = Map.empty,
      bufferSize = 100,
      overflowStrategy = OverflowStrategy.fail,
    ).preMaterialize()
  }

  /** Creates a lobby flow. */
  def createLobbyFlow(
    pushSource: Source[PushOut, NotUsed],
    sessionActor: ActorRef[SessionCommand],
  )(implicit
    materializer: Materializer,
  ): Flow[Message, Message, NotUsed] =
    Flow[Message]
      .filter {
        case _: TextMessage    => true
        case bm: BinaryMessage =>
          // Ignore binary messages, but drain data stream
          bm.dataStream.runWith(Sink.ignore)
          false
      }
      .collect { case tm: TextMessage => tm }
      .mapAsync(parallelism)(_.textStream.runFold("")(_ ++ _))
      .map(decode[In])
      .via(ActorFlow.ask[Either[Error, In], SessionCommand, Option[Out]](sessionActor)(SessionCommand))
      .collect { case Some(out) => out }
      .merge(pushSource)
      .map(_.asJson.noSpaces)
      .map[Message](TextMessage(_))
      .withAttributes(ActorAttributes.supervisionStrategy { e =>
        logger.error("Issue while processing stream", e)
        Supervision.Stop
      })
}
