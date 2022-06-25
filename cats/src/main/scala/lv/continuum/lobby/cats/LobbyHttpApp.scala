package lv.continuum.lobby.cats

import cats.Parallel
import cats.effect.std.Queue
import cats.effect.{Concurrent, Ref}
import cats.syntax.all.*
import fs2.{Pipe, Stream}
import io.odin.Logger
import lv.continuum.lobby.model.{Lobby, ParsingError}
import lv.continuum.lobby.protocol.Protocol.*
import lv.continuum.lobby.protocol.ProtocolFormat
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.*
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text
import org.http4s.{HttpApp, HttpRoutes}

class LobbyHttpApp[F[_]: Concurrent: Logger: Parallel](
  webSocketBuilder: WebSocketBuilder[F],
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
      .collect { case Text(message, _) => fromJson[In](message) }
      .evalMap(lobbySession.process)
      .collect { case Some(out) => out }
      .merge(Stream.fromQueueUnterminated(subscriber))
      .map(out => Text(toJson(out)))
  }

  val app: HttpApp[F] = HttpRoutes
    .of[F] { case GET -> Root / "lobby_api" =>
      for {
        sessionParamsRef <- Ref.of[F, SessionParams](SessionParams())
        subscriber       <- Queue.unbounded[F, PushOut]
        lobbySession = LobbySession(
          authenticator = authenticator,
          lobbyRef = lobbyRef,
          subscribersRef = subscribersRef,
          sessionParamsRef = sessionParamsRef,
          subscriber = subscriber,
        )
        response <- webSocketBuilder.build(pipe(lobbySession, subscriber))
      } yield response
    }
    .orNotFound
}

object LobbyHttpApp {
  def apply[F[_]: Concurrent: Logger: Parallel](
    webSocketBuilder: WebSocketBuilder[F],
    authenticator: Authenticator[F],
    lobbyRef: Ref[F, Lobby],
    subscribersRef: Ref[F, Subscribers[F]],
  ): LobbyHttpApp[F] = new LobbyHttpApp(webSocketBuilder, authenticator, lobbyRef, subscribersRef)
}
