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

  private val pushQueueBufferSize = config.getInt("flow-creator.push-queue-buffer-size")
  log.info(s"Push queue buffer size is $pushQueueBufferSize");

  /**
   * Creates a flow for delivering push notifications to subscribed clients.
   */
  def createPushFlow(implicit materializer: ActorMaterializer) = {
    Source
      .queue(pushQueueBufferSize, OverflowStrategy.backpressure)
      .toMat(BroadcastHub.sink[WebSocketOut])(Keep.both).run()
  }

  /**
   * Creates a lobby flow.
   */
  def createLobbyFlow(pushQueue: SourceQueue[WebSocketOut], pushSource: Source[WebSocketOut, Any])(implicit materializer: ActorMaterializer): Flow[Message, Message, NotUsed] = {
    val clientContext = new ClientContext()

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
      .flatMapConcat {
        case tm => {
          processTextMessage[WebSocketIn, WebSocketOut](tm, LobbyProcessor(pushQueue, clientContext, _), ErrorIn())
        }
      }

      // TODO: Dynamically connect and disconnect push source.
      .merge(pushSource)
      .filter {
        case _: PushNotificationOut if !clientContext.subscribed => false
        case _ => true
      }
      .via(CirceStreamSupport.encode[WebSocketOut])
      .map[Message](TextMessage(_))
  }

  private def processTextMessage[A, B](textMessage: TextMessage, function: A => Option[B], recoverWith: A)(implicit decoder: Decoder[A]) = {
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
  }
}
