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

import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.Future

object FlowCreator {

  private val log = LoggerFactory.getLogger(FlowCreator.getClass)

  private val parallelism = Runtime.getRuntime().availableProcessors() * 2 - 1;
  log.info(s"Parallelism is $parallelism");

  /**
   * Creates a WebSocket communication flow.
   */
  def createLobbyFlow(implicit materializer: ActorMaterializer): Flow[Message, Message, NotUsed] = {
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
        val clientContext = new ClientContext
        tm => clientContext -> tm :: Nil
      })
      .mapAsync(parallelism) {
        case (clientContext, tm) => {
          processTextMessage[WebSocketIn, WebSocketOut](tm, LobbyProcessor(clientContext, _), ErrorIn()).runFold("")(_ ++ _)
        }
      }
      .filter(!_.isEmpty())
      .map[Message](TextMessage(_))
  }

  private def processTextMessage[A, B](textMessage: TextMessage, function: A => B, recoverWith: A)(implicit decoder: Decoder[A], encoder: Encoder[B]): Source[String, _] = {
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
      .via(CirceStreamSupport.encode[B])
  }
}
