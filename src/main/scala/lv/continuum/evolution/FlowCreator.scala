package lv.continuum.evolution

import akka.NotUsed
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream._
import akka.stream.scaladsl._
import com.typesafe.scalalogging.LazyLogging
import de.knutwalker.akka.stream.support.CirceStreamSupport
import io.circe.parser._
import lv.continuum.evolution.model._
import lv.continuum.evolution.processor._

object FlowCreator extends Configurable with LazyLogging {

  private val parallelism = Runtime.getRuntime.availableProcessors() * 2 - 1
  logger.info(s"Parallelism is $parallelism")

  private val pushQueueBufferSize = config.getInt("flow-creator.push-queue-buffer-size")
  logger.info(s"Push queue buffer size is $pushQueueBufferSize")

  /** Creates a flow for delivering push notifications to subscribed clients. */
  def createPushFlow(implicit
    materializer: ActorMaterializer,
  ): (SourceQueueWithComplete[WebSocketOut], Source[WebSocketOut, NotUsed]) =
    Source
      .queue(pushQueueBufferSize, OverflowStrategy.backpressure)
      .toMat(BroadcastHub.sink[WebSocketOut])(Keep.both).run()

  /** Creates a lobby flow. */
  def createLobbyFlow(
    pushQueue: SourceQueue[WebSocketOut],
    pushSource: Source[WebSocketOut, Any],
    clientContext: ClientContext,
  )(implicit
    materializer: ActorMaterializer,
  ): Flow[Message, Message, NotUsed] =
    Flow[Message]
      .filter {
        case _: TextMessage    => true
        case bm: BinaryMessage =>

          // Ignore binary messages, but drain data stream
          bm.dataStream.runWith(Sink.ignore)
          false
      }
      .collect {
        case tm: TextMessage => tm
      }
      .mapAsync(parallelism) {
        tm => tm.textStream.runFold("")(_ ++ _)
      }
      .map(decode[WebSocketIn](_))
      .map {
        case Right(webSocketIn) => LobbyProcessor(pushQueue, clientContext, webSocketIn)
        case Left(error)        =>
          logger.warn(s"Issue while parsing JSON: ${ error.getMessage }")
          Some(ErrorOut("invalid_message"))
      }
      .collect {
        case Some(b) => b
      }

      // TODO: Dynamically connect and disconnect push source.
      .merge(pushSource)
      .filter {
        case _: PushNotificationOut if !clientContext.subscribed => false
        case _                                                   => true
      }
      .via(CirceStreamSupport.encode[WebSocketOut])
      .map[Message](TextMessage(_))
}
