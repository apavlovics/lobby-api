package lv.continuum.evolution.io

import cats.effect.Concurrent
import cats.implicits._
import fs2.Pipe
import fs2.concurrent.Queue
import io.circe.parser._
import io.circe.syntax._
import lv.continuum.evolution.protocol.Protocol._
import lv.continuum.evolution.protocol.ProtocolFormat
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text

class LobbyHttpApp[F[_] : Concurrent](
  lobbyProcessor: LobbyProcessor[F],
) extends Http4sDsl[F]
  with ProtocolFormat {

  private val pipe: Pipe[F, WebSocketFrame, WebSocketFrame] = { inputStream =>
    inputStream
      .collect { case Text(message, _) => decode[In](message) }
      .evalMap(lobbyProcessor.process)
      .collect { case Some(out) => Text(out.asJson.noSpaces) }
  }

  val app: HttpApp[F] = HttpRoutes.of[F] {
    case GET -> Root / "lobby_api" =>
      Queue
        .unbounded[F, WebSocketFrame]
        .flatMap { queue =>
          val send = queue.dequeue.through(pipe)
          val receive = queue.enqueue
          WebSocketBuilder[F].build(send, receive)
        }
  }.orNotFound
}

object LobbyHttpApp {
  def apply[F[_] : Concurrent](
    lobbyProcessor: LobbyProcessor[F],
  ): LobbyHttpApp[F] = new LobbyHttpApp(lobbyProcessor)
}
