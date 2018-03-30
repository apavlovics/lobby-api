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
   * Creates an echo flow.
   *
   * Incoming messages from a particular WebSocket client are passed through the server,
   * then outgoing messages are sent back to that WebSocket client.
   */
  def createEchoFlow(implicit materializer: ActorMaterializer): Flow[Message, Message, NotUsed] = {
    implicit val flowName = "Echo"

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
      .mapAsync(parallelism) {
        case tm: TextMessage => {
          processTextMessage[SampleIn, SampleOut](tm, SampleProcessor(_, flowName), SampleIn(content = "Invalid message")).runFold("")(_ ++ _)
        }
      }
      .filter(!_.isEmpty())
      .map[Message](TextMessage(_))
  }

  /**
   * Creates a broadcast flow.
   *
   * Incoming messages from all WebSocket clients are passed through the merge
   * and broadcast hubs, then outgoing messages are sent back to all WebSocket clients.
   */
  def createBroadcastFlow(implicit materializer: ActorMaterializer): Flow[Message, Message, NotUsed] = {
    implicit val flowName = "Broadcast"

    val (broadcastSink, broadcastSource) =
      MergeHub.source[String].toMat(BroadcastHub.sink[String])(Keep.both).run()

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
      .mapAsync(parallelism) {
        case tm: TextMessage => {
          processTextMessage[SampleIn, SampleOut](tm, SampleProcessor(_, flowName), SampleIn(content = "Invalid message")).runFold("")(_ ++ _)
        }
      }
      .via(Flow.fromSinkAndSource(broadcastSink, broadcastSource))
      .filter(!_.isEmpty())
      .map[Message](TextMessage(_))
  }

  /**
   * Creates a push flow.
   *
   * Incoming messages from a particular WebSocket client are ignored, while
   * outgoing messages are pushed by the server to that WebSocket client every second.
   */
  def createPushFlow(implicit materializer: ActorMaterializer): Flow[Message, Message, NotUsed] = {
    val pushSource = Source
      .tick(1.second, 1.second, () => Tick())
      .map {
        case _ => SampleOut(content = "Push message")
      }
      .via(CirceStreamSupport.encode[SampleOut])
      .map(TextMessage(_))

    Flow[Message]
      .mapConcat {
        case tm: TextMessage => {

          // Ignore text messages, but drain data stream
          tm.textStream.runWith(Sink.ignore)
          Nil
        }
        case bm: BinaryMessage => {

          // Ignore binary messages, but drain data stream
          bm.dataStream.runWith(Sink.ignore)
          Nil
        }
      }
      .merge(pushSource)
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
