package lv.continuum.lobby.cats

import cats.Parallel
import cats.effect.std.Queue
import cats.effect.{Concurrent, Ref}
import cats.syntax.all._
import fs2.{Pipe, Stream}
import io.circe.parser._
import io.circe.syntax._
import io.odin.Logger
import lv.continuum.lobby.model.Lobby
import lv.continuum.lobby.protocol.Protocol._
import lv.continuum.lobby.protocol.ProtocolFormat
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text
import org.http4s.{HttpApp, HttpRoutes}

class LobbyHttpApp[F[_]: Concurrent: Logger: Parallel](
  authenticator: Authenticator[F],
  lobbyRef: Ref[F, Lobby],
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
      .merge(Stream.fromQueueUnterminated(subscriber))
      .map(out => Text(out.asJson.noSpaces))
  }

  val app: HttpApp[F] = HttpRoutes
    .of[F] { case GET -> Root / "lobby_api" =>
      for {
        sessionParamsRef <- Ref.of[F, SessionParams](SessionParams())
        queue            <- Queue.unbounded[F, WebSocketFrame]
        subscriber       <- Queue.unbounded[F, PushOut]
        lobbySession = LobbySession(
          authenticator = authenticator,
          lobbyRef = lobbyRef,
          subscribersRef = subscribersRef,
          sessionParamsRef = sessionParamsRef,
          subscriber = subscriber,
        )
        response <- WebSocketBuilder[F].build(pipe(lobbySession, subscriber))
      } yield response
    }
    .orNotFound
}

object LobbyHttpApp {
  def apply[F[_]: Concurrent: Logger: Parallel](
    authenticator: Authenticator[F],
    lobbyRef: Ref[F, Lobby],
    subscribersRef: Ref[F, Subscribers[F]],
  ): LobbyHttpApp[F] = new LobbyHttpApp(authenticator, lobbyRef, subscribersRef)
}
