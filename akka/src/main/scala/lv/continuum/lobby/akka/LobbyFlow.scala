package lv.continuum.lobby.akka

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import io.circe.Error
import io.circe.parser._
import io.circe.syntax._
import lv.continuum.lobby.akka.SessionActor.SessionCommand
import lv.continuum.lobby.protocol.Protocol._
import lv.continuum.lobby.protocol._

import scala.concurrent.duration._

/** A complete lobby flow. */
object LobbyFlow extends ProtocolFormat with LazyLogging {

  private val parallelism = Runtime.getRuntime.availableProcessors() * 2 - 1
  logger.info(s"Parallelism is $parallelism")

  implicit private val timeout: Timeout = Timeout(5.seconds)
  logger.info(s"Timeout is ${timeout.duration}")

  def apply(
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
