package lv.continuum.evolution.io

import cats.effect.Concurrent
import cats.implicits._
import fs2._
import fs2.concurrent.Queue
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text

class LobbyHttpApp[F[_] : Concurrent] extends Http4sDsl[F] {

  def app: HttpApp[F] = HttpRoutes.of[F] {

    case GET -> Root / "lobby_api" =>
      val echoPipe: Pipe[F, WebSocketFrame, WebSocketFrame] =
        _.collect {
          case Text(message, _) => Text(s"Received $message")
          case _                => Text("Received unexpected message")
        }

      Queue
        .unbounded[F, WebSocketFrame]
        .flatMap { queue =>
          val send = queue.dequeue.through(echoPipe)
          val receive = queue.enqueue
          WebSocketBuilder[F].build(send, receive)
        }
  }.orNotFound
}

object LobbyHttpApp {
  def apply[F[_] : Concurrent]: LobbyHttpApp[F] = new LobbyHttpApp[F]
}
