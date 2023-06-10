package lv.continuum.lobby.zio

import zio.ZIO
import zio.http.*
import zio.http.ChannelEvent.*

object LobbyHttpApp {

  private val socketApp: SocketApp[Any] = Handler.webSocket { channel =>
    channel.receiveAll {
      case Read(WebSocketFrame.Text(message)) =>
        channel.send(Read(WebSocketFrame.Text(message)))
      case _ => ZIO.unit
    }
  }

  val app: App[Any] = Http
    .collectZIO[Request] { case Method.GET -> Root / "lobby_api" => socketApp.toResponse }
}
