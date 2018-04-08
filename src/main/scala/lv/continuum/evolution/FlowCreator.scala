package lv.continuum.evolution

import akka.NotUsed
import akka.http.scaladsl.model.ws.{ Message, TextMessage, BinaryMessage }
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString

import de.knutwalker.akka.stream.support.CirceStreamSupport
import de.knutwalker.akka.stream.support.CirceStreamSupport._

import jawn.ParseException

import io.circe._
import io.circe.syntax._

import java.time.LocalDateTime

import lv.continuum.evolution.model._
import lv.continuum.evolution.processor._

import scala.concurrent.duration._
import scala.concurrent.Future

object FlowCreator extends Configurable with Loggable {

  private val parallelism = Runtime.getRuntime().availableProcessors() * 2 - 1;
  log.info(s"Parallelism is $parallelism");

  private val pushQueueBufferSize = config.getInt("flow-creator.push-queue-buffer-size")
  log.info(s"Push queue buffer size is $pushQueueBufferSize");

  /**
   * Creates a lobby flow.
   */
  def createLobbyFlow(implicit materializer: ActorMaterializer): Flow[Message, Message, NotUsed] = {
    val (pushQueue, broadcastSource) = Source
      .queue(pushQueueBufferSize, OverflowStrategy.backpressure)
      .via(CirceStreamSupport.encode[WebSocketOut])
      .toMat(BroadcastHub.sink[String])(Keep.both).run()

    Flow[Message]
      .filter(_ match {
        case tm: TextMessage => true
        case bm: BinaryMessage => {

          // Ignore binary messages, but drain data stream
          bm.dataStream.runWith(Sink.ignore)
          false
        }
      })
      .collect {
        case tm: TextMessage => tm
      }
      .statefulMapConcat(() => {

        // Add client context to store identity information
        val clientContext = new ClientContext
        tm => (clientContext -> tm) :: Nil
      })
      .mapAsync(parallelism) {
        case (clientContext, tm) => {
          processTextMessage[WebSocketIn, WebSocketOut](tm, LobbyProcessor(pushQueue, clientContext, _), ErrorIn()).runFold("")(_ ++ _)
        }
      }

      // TODO: Broadcast only to subscribed clients.
      .merge(broadcastSource)
      .filter(!_.isEmpty())
      .map[Message](TextMessage(_))
  }

  private def processTextMessage[A, B](textMessage: TextMessage, function: A => Option[B], recoverWith: A)(implicit decoder: Decoder[A], encoder: Encoder[B]): Source[String, _] = {
    textMessage.textStream
      .map(ByteString(_))
      .via(CirceStreamSupport.decode[A])
      .recover {
        case e @ (_: JsonParsingException | _: ParseException) => {
          log.warn(s"Issue while parsing JSON: ${e.getMessage}")
          recoverWith
        }
      }
      .map(function)
      .collect {
        case Some(b) => b
      }
      .via(CirceStreamSupport.encode[B])
  }
}
