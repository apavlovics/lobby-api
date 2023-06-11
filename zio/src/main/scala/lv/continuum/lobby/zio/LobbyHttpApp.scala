package lv.continuum.lobby.zio

import lv.continuum.lobby.protocol.Protocol.In
import lv.continuum.lobby.protocol.ProtocolFormat
import lv.continuum.lobby.zio.layer.{Authenticator, LobbyHolder, SessionHolderLive}
import zio.*
import zio.http.*
import zio.http.ChannelEvent.Read
import zio.http.WebSocketFrame.Text

object LobbyHttpApp extends ProtocolFormat {

  private type Env = Authenticator & LobbyHolder

  private val socketApp: SocketApp[Env] = Handler.webSocket { channel =>
    channel
      .receiveAll {
        case Read(Text(message)) =>
          for {
            in  <- ZIO.attempt(fromJson[In](message))
            out <- LobbySession.process(in)
            _   <- out.fold(ZIO.unit) { out => channel.send(Read(Text(toJson(out)))) }
          } yield ()
        case _ => ZIO.unit
      }
      .provideSome[Env](SessionHolderLive.layer)
  }

  val app: App[Env] = Http
    .collectZIO[Request] { case Method.GET -> Root / "lobby_api" => socketApp.toResponse }
}
