package lv.continuum.evolution.akka

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import io.circe.parser._
import io.circe.syntax._
import lv.continuum.evolution.akka.SessionActor.Command
import lv.continuum.evolution.protocol.Protocol._
import lv.continuum.evolution.protocol._

import scala.concurrent.duration._

object FlowCreator
  extends Configurable
    with ProtocolFormat
    with LazyLogging {

  private val parallelism = Runtime.getRuntime.availableProcessors() * 2 - 1
  logger.info(s"Parallelism is $parallelism")

  private val pushQueueBufferSize = config.getInt("flow-creator.push-queue-buffer-size")
  logger.info(s"Push queue buffer size is $pushQueueBufferSize")

  /** Creates a flow for delivering push notifications to subscribed clients. */
  def createPushFlow(implicit
    materializer: Materializer,
  ): (SourceQueueWithComplete[Out], Source[Out, NotUsed]) =
    Source
      .queue(pushQueueBufferSize, OverflowStrategy.backpressure)
      .toMat(BroadcastHub.sink[Out])(Keep.both).run()

  /** Creates a lobby flow. */
  def createLobbyFlow(
    pushQueue: SourceQueue[Out],
    pushSource: Source[Out, NotUsed],
    sessionActorRef: ActorRef[Command],
  )(implicit
    materializer: Materializer,
  ): Flow[Message, Message, NotUsed] = {

    implicit val timeout: Timeout = Timeout(5.seconds)

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
      .via(ActorFlow.ask(sessionActorRef)((in, replyTo: ActorRef[Out]) => Command(in, replyTo)))
      .merge(pushSource)
      .map(_.asJson.noSpaces)
      .map[Message](TextMessage(_))
      .withAttributes(ActorAttributes.supervisionStrategy { e =>
        logger.error("Issue while processing stream", e)
        Supervision.Stop
      })
  }
}
