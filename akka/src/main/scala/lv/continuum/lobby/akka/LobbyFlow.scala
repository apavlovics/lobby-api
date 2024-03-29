package lv.continuum.lobby.akka

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.*
import akka.stream.scaladsl.*
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import lv.continuum.lobby.akka.SessionActor.SessionCommand
import lv.continuum.lobby.model.ParsingError
import lv.continuum.lobby.protocol.Protocol.*
import lv.continuum.lobby.protocol.*

import scala.concurrent.duration.*

/** A complete lobby flow. */
object LobbyFlow extends ProtocolFormat with LazyLogging {

  private val parallelism = Runtime.getRuntime.availableProcessors() * 2 - 1
  logger.info(s"Parallelism is $parallelism")

  private given timeout: Timeout = Timeout(5.seconds)
  logger.info(s"Timeout is ${timeout.duration}")

  def apply(
    pushSource: Source[PushOut, NotUsed],
    sessionActor: ActorRef[SessionCommand],
  )(using
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
      .map(fromJson[In])
      .via(
        ActorFlow.ask[Either[ParsingError, In], SessionCommand, Option[Out]](
          ref = sessionActor,
        )(
          makeMessage = SessionCommand.apply,
        )
      )
      .collect { case Some(out) => out }
      .merge(pushSource)
      .map(toJson)
      .map[Message](TextMessage(_))
      .withAttributes(ActorAttributes.supervisionStrategy { e =>
        logger.error("Issue while processing stream", e)
        Supervision.Stop
      })
}
