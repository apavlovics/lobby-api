package lv.continuum.evolution.cats

import cats.Parallel
import cats.effect.Concurrent
import cats.effect.concurrent.Ref
import cats.implicits._
import fs2.Pipe
import fs2.concurrent.Queue
import io.circe.parser._
import io.circe.syntax._
import io.odin.Logger
import lv.continuum.evolution.protocol.Protocol._
import lv.continuum.evolution.protocol.ProtocolFormat
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text
import org.http4s.{HttpApp, HttpRoutes}

class LobbyHttpApp[F[_] : Concurrent : Logger : Parallel](
  tablesRef: Ref[F, Tables],
  subscribersRef: Ref[F, Subscribers[F]],
) extends Http4sDsl[F]
  with ProtocolFormat {

  private def pipe(
    lobbySession: LobbySession[F],
    subscriber: Subscriber[F],
  ): Pipe[F, WebSocketFrame, WebSocketFrame] = { inputStream =>
    inputStream
      .collect { case Text(message, _) => decode[In](message) }
      .evalMap(lobbySession.process)
      .collect { case Some(out) => out }
      .merge(subscriber.dequeue)
      .map(out => Text(out.asJson.noSpaces))
  }

  val app: HttpApp[F] = HttpRoutes.of[F] {
    case GET -> Root / "lobby_api" =>
      for {
        sessionParamsRef <- Ref.of[F, SessionParams](SessionParams())
        queue <- Queue.unbounded[F, WebSocketFrame]
        subscriber <- Queue.unbounded[F, PushOut]
        lobbySession = LobbySession(tablesRef, subscribersRef, sessionParamsRef, subscriber)

        send = queue.dequeue.through(pipe(lobbySession, subscriber))
        receive = queue.enqueue
        response <- WebSocketBuilder[F].build(send, receive)
      } yield response
  }.orNotFound
}

object LobbyHttpApp {
  def apply[F[_] : Concurrent : Logger : Parallel](
    tablesRef: Ref[F, Tables],
    subscribersRef: Ref[F, Subscribers[F]],
  ): LobbyHttpApp[F] = new LobbyHttpApp(tablesRef, subscribersRef)
}
